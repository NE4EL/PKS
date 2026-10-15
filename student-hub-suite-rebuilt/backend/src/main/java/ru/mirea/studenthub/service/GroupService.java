package ru.mirea.studenthub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.studenthub.domain.StudyGroup;
import ru.mirea.studenthub.dto.GroupDtos.GroupRequest;
import ru.mirea.studenthub.dto.GroupDtos.GroupResponse;
import ru.mirea.studenthub.error.BadRequestException;
import ru.mirea.studenthub.error.ConflictException;
import ru.mirea.studenthub.error.NotFoundException;
import ru.mirea.studenthub.repository.StudyGroupRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Прикладные операции над учебными группами. */
@Service
public class GroupService {
    private final StudyGroupRepository groups;
    private final AuditService audit;

    public GroupService(StudyGroupRepository groups, AuditService audit) {
        this.groups = groups;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<GroupResponse> list(boolean activeOnly) {
        return (activeOnly ? groups.findByActiveTrueOrderByCodeAsc() : groups.findAllByOrderByCodeAsc())
                .stream().map(GroupService::toResponse).toList();
    }

    @Transactional
    public GroupResponse create(GroupRequest request) {
        String code = normalizeCode(request.code());
        if (groups.existsByCodeIgnoreCase(code)) throw new ConflictException("Группа с таким кодом уже существует");
        StudyGroup group = new StudyGroup(UUID.randomUUID(), code, normalizeNullable(request.name()), request.admissionYear());
        if (request.active() != null) group.setActive(request.active());
        groups.save(group);
        audit.record("GROUP_CREATED", "GROUP", group.getId(), "Создана группа " + code, Map.of());
        return toResponse(group);
    }

    @Transactional
    public GroupResponse update(UUID id, GroupRequest request) {
        StudyGroup group = require(id);
        if (request.version() == null || request.version() != group.getVersion())
            throw new ConflictException("Группа уже изменена. Обновите список");
        String code = normalizeCode(request.code());
        groups.findByCodeIgnoreCase(code).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Группа с таким кодом уже существует");
        });
        group.setCode(code);
        group.setName(normalizeNullable(request.name()));
        group.setAdmissionYear(request.admissionYear());
        if (request.active() != null) group.setActive(request.active());
        audit.record("GROUP_UPDATED", "GROUP", id, "Изменена группа " + code, Map.of());
        return toResponse(group);
    }

    @Transactional
    public GroupResponse setActive(UUID id, boolean active, long version) {
        StudyGroup group = require(id);
        if (version != group.getVersion()) throw new ConflictException("Группа уже изменена. Обновите список");
        group.setActive(active);
        audit.record(active ? "GROUP_ACTIVATED" : "GROUP_DEACTIVATED", "GROUP", id,
                (active ? "Активирована" : "Деактивирована") + " группа " + group.getCode(), Map.of());
        return toResponse(group);
    }

    private StudyGroup require(UUID id) {
        return groups.findById(id).orElseThrow(() -> new NotFoundException("Группа не найдена"));
    }

    private static GroupResponse toResponse(StudyGroup group) {
        return new GroupResponse(group.getId(), group.getCode(), group.getName(), group.getAdmissionYear(),
                group.isActive(), group.getCreatedAt(), group.getUpdatedAt(), group.getVersion());
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Код группы обязателен");
        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }
}
