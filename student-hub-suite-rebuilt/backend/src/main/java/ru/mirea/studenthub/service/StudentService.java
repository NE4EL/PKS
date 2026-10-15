package ru.mirea.studenthub.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.studenthub.config.AppProperties;
import ru.mirea.studenthub.domain.Student;
import ru.mirea.studenthub.domain.StudentStatus;
import ru.mirea.studenthub.domain.StudyGroup;
import ru.mirea.studenthub.domain.UserAccount;
import ru.mirea.studenthub.dto.StudentDtos.BulkAction;
import ru.mirea.studenthub.dto.StudentDtos.BulkFailure;
import ru.mirea.studenthub.dto.StudentDtos.BulkRequest;
import ru.mirea.studenthub.dto.StudentDtos.BulkResult;
import ru.mirea.studenthub.dto.StudentDtos.StudentRequest;
import ru.mirea.studenthub.dto.StudentDtos.StudentResponse;
import ru.mirea.studenthub.error.BadRequestException;
import ru.mirea.studenthub.error.ConflictException;
import ru.mirea.studenthub.error.NotFoundException;
import ru.mirea.studenthub.filter.SmartFilterParser;
import ru.mirea.studenthub.filter.StudentFilter;
import ru.mirea.studenthub.filter.StudentSpecifications;
import ru.mirea.studenthub.repository.StudentNoteRepository;
import ru.mirea.studenthub.repository.StudentRepository;
import ru.mirea.studenthub.repository.StudyGroupRepository;
import ru.mirea.studenthub.security.CurrentUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Основной прикладной сервис карточек студентов. */
@Service
public class StudentService {
    private final StudentRepository students;
    private final StudentNoteRepository notes;
    private final StudyGroupRepository groups;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final StudentMapper mapper;
    private final RiskCalculator riskCalculator;
    private final AuditService audit;
    private final AppProperties properties;

    public StudentService(StudentRepository students, StudentNoteRepository notes,
                          StudyGroupRepository groups, CurrentUserService currentUser,
                          SmartFilterParser parser, StudentMapper mapper,
                          RiskCalculator riskCalculator, AuditService audit,
                          AppProperties properties) {
        this.students = students;
        this.notes = notes;
        this.groups = groups;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.riskCalculator = riskCalculator;
        this.audit = audit;
        this.properties = properties;
    }

    /** Выполняет безопасный умный поиск с пагинацией. */
    @Transactional(readOnly = true)
    public Page<StudentResponse> search(String query, Pageable pageable) {
        StudentFilter filter = parser.parse(query);
        return students.findAll(StudentSpecifications.from(filter), pageable).map(mapper::toResponse);
    }

    /** Возвращает одну карточку независимо от архивного статуса. */
    @Transactional(readOnly = true)
    public StudentResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    /** Создаёт карточку и рассчитывает риск на сервере. */
    @Transactional
    public StudentResponse create(StudentRequest request) {
        UserAccount actor = currentUser.requireEntity();
        validateExternalId(request.externalId(), null);
        Student student = new Student(UUID.randomUUID(), normalizeRequired(request.firstName()),
                normalizeRequired(request.lastName()), actor);
        apply(student, request, actor);
        students.save(student);
        audit.record("STUDENT_CREATED", "STUDENT", student.getId(),
                "Создана карточка студента " + displayName(student), Map.of());
        return mapper.toResponse(student);
    }

    /** Изменяет карточку только при совпадении optimistic version. */
    @Transactional
    public StudentResponse update(UUID id, StudentRequest request) {
        Student student = require(id);
        verifyVersion(request.version(), student.getVersion());
        validateExternalId(request.externalId(), id);
        UserAccount actor = currentUser.requireEntity();
        apply(student, request, actor);
        audit.record("STUDENT_UPDATED", "STUDENT", id,
                "Изменена карточка студента " + displayName(student), Map.of("version", student.getVersion()));
        return mapper.toResponse(student);
    }

    /** Архивирует карточку вместо физического удаления. */
    @Transactional
    public StudentResponse archive(UUID id) {
        Student student = require(id);
        archiveInternal(student, currentUser.requireEntity());
        audit.record("STUDENT_ARCHIVED", "STUDENT", id,
                "Карточка студента перемещена в архив", Map.of());
        return mapper.toResponse(student);
    }

    /** Возвращает карточку из архива. */
    @Transactional
    public StudentResponse restore(UUID id) {
        Student student = require(id);
        student.setArchived(false);
        student.setArchivedAt(null);
        student.setArchivedBy(null);
        student.setUpdatedBy(currentUser.requireEntity());
        audit.record("STUDENT_RESTORED", "STUDENT", id,
                "Карточка студента восстановлена из архива", Map.of());
        return mapper.toResponse(student);
    }

    /**
     * Безвозвратно удаляет запись только при включённом feature flag и явном подтверждении.
     */
    @Transactional
    public void permanentDelete(UUID id, String confirmation) {
        if (!properties.permanentDeleteEnabled()) {
            throw new BadRequestException("Безвозвратное удаление отключено конфигурацией сервера");
        }
        if (!"PERMANENTLY_DELETE".equals(confirmation)) {
            throw new BadRequestException("Для удаления требуется заголовок X-Confirm-Delete: PERMANENTLY_DELETE");
        }
        Student student = require(id);
        String name = displayName(student);
        students.delete(student);
        audit.record("STUDENT_PERMANENTLY_DELETED", "STUDENT", id,
                "Безвозвратно удалена карточка " + name, Map.of());
    }

    /** Выполняет ограниченную массовую операцию и сообщает ошибки отдельных UUID. */
    @Transactional
    public BulkResult bulk(BulkRequest request) {
        validateBulkRequest(request);
        UserAccount actor = currentUser.requireEntity();
        int succeeded = 0;
        var failures = new ArrayList<BulkFailure>();
        for (UUID id : request.ids()) {
            Student student = students.findById(id).orElse(null);
            if (student == null) {
                failures.add(new BulkFailure(id, "Студент не найден"));
                continue;
            }
            try {
                applyBulk(student, request, actor);
                succeeded++;
            } catch (RuntimeException ex) {
                failures.add(new BulkFailure(id, ex.getMessage()));
            }
        }
        audit.record("STUDENT_BULK_" + request.action().name(), "STUDENT", null,
                "Выполнена массовая операция", Map.of("requested", request.ids().size(), "succeeded", succeeded));
        return new BulkResult(request.ids().size(), succeeded, failures);
    }

    /** Используется CSV-сервисом внутри транзакции без дублирования правил. */
    Student upsertFromImport(Student existing, StudentRequest request, UserAccount actor) {
        if (existing == null) {
            Student created = new Student(UUID.randomUUID(), normalizeRequired(request.firstName()),
                    normalizeRequired(request.lastName()), actor);
            apply(created, request, actor);
            return students.save(created);
        }
        apply(existing, request, actor);
        return existing;
    }

    private void apply(Student student, StudentRequest request, UserAccount actor) {
        student.setExternalId(normalizeNullable(request.externalId()));
        student.setFirstName(normalizeRequired(request.firstName()));
        student.setLastName(normalizeRequired(request.lastName()));
        student.setMiddleName(normalizeNullable(request.middleName()));
        student.setEmail(normalizeEmail(request.email()));
        student.setPhone(normalizeNullable(request.phone()));
        student.setBirthDate(request.birthDate());
        student.setGroup(resolveGroup(request.groupId()));
        student.setStatus(request.status());
        student.setAverageGrade(request.averageGrade());
        student.setHasDebt(request.hasDebt());
        student.setTags(normalizeTags(request.tags()));
        student.setRiskLevel(riskCalculator.calculate(request.status(), request.averageGrade(), request.hasDebt()));
        student.setUpdatedBy(actor);
    }

    private void applyBulk(Student student, BulkRequest request, UserAccount actor) {
        switch (request.action()) {
            case ARCHIVE -> archiveInternal(student, actor);
            case RESTORE -> {
                student.setArchived(false);
                student.setArchivedAt(null);
                student.setArchivedBy(null);
                student.setUpdatedBy(actor);
            }
            case SET_STATUS -> {
                student.setStatus(request.status());
                student.setRiskLevel(riskCalculator.calculate(student.getStatus(), student.getAverageGrade(), student.isHasDebt()));
                student.setUpdatedBy(actor);
            }
            case ADD_TAG -> {
                Set<String> tags = new LinkedHashSet<>(student.getTags());
                tags.add(normalizeTag(request.tag()));
                student.setTags(tags);
                student.setUpdatedBy(actor);
            }
            case REMOVE_TAG -> {
                Set<String> tags = new LinkedHashSet<>(student.getTags());
                tags.remove(normalizeTag(request.tag()));
                student.setTags(tags);
                student.setUpdatedBy(actor);
            }
        }
    }

    private static void validateBulkRequest(BulkRequest request) {
        if (request.action() == BulkAction.SET_STATUS && request.status() == null) {
            throw new BadRequestException("Для SET_STATUS требуется status");
        }
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Для операции с тегом требуется tag");
        }
    }

    private static void archiveInternal(Student student, UserAccount actor) {
        student.setArchived(true);
        student.setArchivedAt(Instant.now());
        student.setArchivedBy(actor);
        student.setUpdatedBy(actor);
    }

    private Student require(UUID id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Студент не найден"));
    }

    private StudyGroup resolveGroup(UUID id) {
        if (id == null) return null;
        StudyGroup group = groups.findById(id).orElseThrow(() -> new BadRequestException("Учебная группа не найдена"));
        if (!group.isActive()) throw new BadRequestException("Нельзя назначить неактивную группу");
        return group;
    }

    private void validateExternalId(String externalId, UUID currentId) {
        String normalized = normalizeNullable(externalId);
        if (normalized == null) return;
        boolean exists = currentId == null
                ? students.existsByExternalIdIgnoreCase(normalized)
                : students.existsByExternalIdIgnoreCaseAndIdNot(normalized, currentId);
        if (exists) throw new ConflictException("Внешний идентификатор уже используется");
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null) throw new BadRequestException("Для изменения требуется поле version");
        if (requested != actual) throw new ConflictException("Карточка уже изменена. Обновите данные и повторите операцию");
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) result.add(normalizeTag(tag));
        if (result.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
        return result;
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 50) throw new BadRequestException("Тег длиннее 50 символов");
        return normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Обязательное текстовое поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static String displayName(Student student) {
        return student.getLastName() + " " + student.getFirstName();
    }
}
