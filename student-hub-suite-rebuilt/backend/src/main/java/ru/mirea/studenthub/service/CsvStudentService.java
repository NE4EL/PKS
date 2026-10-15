package ru.mirea.studenthub.service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ru.mirea.studenthub.config.AppProperties;
import ru.mirea.studenthub.domain.Student;
import ru.mirea.studenthub.domain.StudentStatus;
import ru.mirea.studenthub.domain.StudyGroup;
import ru.mirea.studenthub.domain.UserAccount;
import ru.mirea.studenthub.dto.ImportDtos.ImportMode;
import ru.mirea.studenthub.dto.ImportDtos.ImportPreview;
import ru.mirea.studenthub.dto.ImportDtos.ImportResult;
import ru.mirea.studenthub.dto.ImportDtos.RowIssue;
import ru.mirea.studenthub.dto.StudentDtos.StudentRequest;
import ru.mirea.studenthub.dto.StudentDtos.StudentResponse;
import ru.mirea.studenthub.error.BadRequestException;
import ru.mirea.studenthub.filter.SmartFilterParser;
import ru.mirea.studenthub.filter.StudentSpecifications;
import ru.mirea.studenthub.repository.StudentRepository;
import ru.mirea.studenthub.repository.StudyGroupRepository;
import ru.mirea.studenthub.security.CurrentUserService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Защищённый CSV import/export.
 *
 * <p>Предварительная проверка отделена от импорта: ошибки видны до записи в базу.
 * При экспорте нейтрализуются значения, которые Excel может воспринять как формулы.</p>
 */
@Service
public class CsvStudentService {
    private static final String[] HEADERS = {
            "externalId", "lastName", "firstName", "middleName", "email", "phone",
            "birthDate", "group", "status", "averageGrade", "hasDebt", "tags"
    };

    private final StudentRepository students;
    private final StudyGroupRepository groups;
    private final StudentService studentService;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final StudentMapper mapper;
    private final Validator validator;
    private final AppProperties properties;
    private final AuditService audit;

    public CsvStudentService(StudentRepository students, StudyGroupRepository groups,
                             StudentService studentService, CurrentUserService currentUser,
                             SmartFilterParser parser, StudentMapper mapper, Validator validator,
                             AppProperties properties, AuditService audit) {
        this.students = students;
        this.groups = groups;
        this.studentService = studentService;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.validator = validator;
        this.properties = properties;
        this.audit = audit;
    }

    /** Проверяет CSV без записи в базу. */
    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        ParsedFile parsed = parse(file, mode == null ? ImportMode.CREATE_ONLY : mode);
        return new ImportPreview(parsed.totalRows(), parsed.rows().size(), parsed.issues().size(), parsed.issues());
    }

    /** Применяет только валидные строки и возвращает детальный итог. */
    @Transactional
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        ImportMode effectiveMode = mode == null ? ImportMode.CREATE_ONLY : mode;
        ParsedFile parsed = parse(file, effectiveMode);
        UserAccount actor = currentUser.requireEntity();
        int created = 0;
        int updated = 0;
        int skipped = parsed.issues().size();
        List<RowIssue> runtimeIssues = new ArrayList<>(parsed.issues());

        for (ParsedRow row : parsed.rows()) {
            try {
                Student existing = students.findByExternalIdIgnoreCase(row.request().externalId()).orElse(null);
                if (existing != null && effectiveMode == ImportMode.CREATE_ONLY) {
                    runtimeIssues.add(new RowIssue(row.rowNumber(), "externalId", "Запись уже существует"));
                    skipped++;
                    continue;
                }
                studentService.upsertFromImport(existing, row.request(), actor);
                if (existing == null) created++; else updated++;
            } catch (RuntimeException ex) {
                runtimeIssues.add(new RowIssue(row.rowNumber(), "row", safeMessage(ex)));
                skipped++;
            }
        }
        audit.record("STUDENT_CSV_IMPORTED", "STUDENT", null, "Импортирован CSV со студентами",
                Map.of("created", created, "updated", updated, "skipped", skipped, "mode", effectiveMode.name()));
        return new ImportResult(parsed.totalRows(), created, updated, skipped, List.copyOf(runtimeIssues));
    }

    /** Потоково экспортирует текущую выборку без загрузки всего набора в память. */
    @Transactional(readOnly = true)
    public void export(String query, OutputStream output) throws IOException {
        // BOM записывается до создания CSVPrinter, иначе заголовок окажется перед BOM.
        output.write("\ufeff".getBytes(StandardCharsets.UTF_8));
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
            int page = 0;
            while (true) {
                var result = students.findAll(StudentSpecifications.from(parser.parse(query)),
                        PageRequest.of(page, 500, Sort.by("lastName").ascending().and(Sort.by("firstName").ascending())));
                for (Student student : result.getContent()) {
                    StudentResponse dto = mapper.toResponse(student);
                    printer.printRecord(
                            safeCell(dto.externalId()), safeCell(dto.lastName()), safeCell(dto.firstName()),
                            safeCell(dto.middleName()), safeCell(dto.email()), safeCell(dto.phone()),
                            dto.birthDate(), safeCell(dto.groupCode()), dto.status(), dto.averageGrade(),
                            dto.hasDebt(), safeCell(String.join("|", dto.tags())));
                }
                if (result.isLast()) break;
                page++;
            }
            printer.flush();
        }
        audit.record("STUDENT_CSV_EXPORTED", "STUDENT", null, "Экспортирована выборка студентов", Map.of("query", query == null ? "" : query));
    }

    private ParsedFile parse(MultipartFile file, ImportMode mode) {
        if (file == null || file.isEmpty()) throw new BadRequestException("Выберите непустой CSV-файл");
        List<ParsedRow> validRows = new ArrayList<>();
        List<RowIssue> issues = new ArrayList<>();
        Set<String> externalIdsInFile = new LinkedHashSet<>();
        int total = 0;

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .get();
        try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csv = CSVParser.parse(reader, format)) {
            validateHeaders(csv.getHeaderMap());
            for (CSVRecord record : csv) {
                total++;
                long rowNumber = record.getRecordNumber() + 1;
                if (total > properties.importLimits().maxRows()) {
                    throw new BadRequestException("CSV содержит больше " + properties.importLimits().maxRows() + " строк");
                }
                try {
                    ParsedRow row = toRow(record, rowNumber);
                    Set<ConstraintViolation<StudentRequest>> violations = validator.validate(row.request());
                    if (!violations.isEmpty()) {
                        for (ConstraintViolation<StudentRequest> violation : violations) {
                            issues.add(new RowIssue(rowNumber, violation.getPropertyPath().toString(), violation.getMessage()));
                        }
                        continue;
                    }
                    String key = row.request().externalId().toLowerCase(Locale.ROOT);
                    if (!externalIdsInFile.add(key)) {
                        issues.add(new RowIssue(rowNumber, "externalId", "Дубликат externalId внутри файла"));
                        continue;
                    }
                    if (mode == ImportMode.CREATE_ONLY && students.existsByExternalIdIgnoreCase(row.request().externalId())) {
                        issues.add(new RowIssue(rowNumber, "externalId", "Запись уже существует"));
                        continue;
                    }
                    validRows.add(row);
                } catch (RuntimeException ex) {
                    issues.add(new RowIssue(rowNumber, "row", safeMessage(ex)));
                }
            }
        } catch (IOException ex) {
            throw new BadRequestException("Не удалось прочитать CSV в UTF-8");
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Некорректная структура CSV: " + ex.getMessage());
        }
        return new ParsedFile(total, List.copyOf(validRows), List.copyOf(issues));
    }

    private ParsedRow toRow(CSVRecord record, long rowNumber) {
        String externalId = required(record, "externalId");
        String groupCode = nullable(record, "group");
        StudyGroup group = groupCode == null ? null : groups.findByCodeIgnoreCase(groupCode)
                .orElseThrow(() -> new BadRequestException("Группа не найдена: " + groupCode));
        if (group != null && !group.isActive()) throw new BadRequestException("Группа неактивна: " + groupCode);

        StudentRequest request = new StudentRequest(
                externalId,
                required(record, "firstName"),
                required(record, "lastName"),
                nullable(record, "middleName"),
                nullable(record, "email"),
                nullable(record, "phone"),
                parseDate(nullable(record, "birthDate")),
                group == null ? null : group.getId(),
                parseStatus(required(record, "status")),
                parseGrade(nullable(record, "averageGrade")),
                parseBoolean(nullable(record, "hasDebt")),
                parseTags(nullable(record, "tags")),
                null);
        return new ParsedRow(rowNumber, request);
    }

    private static void validateHeaders(Map<String, Integer> actual) {
        for (String header : HEADERS) {
            if (!actual.containsKey(header)) throw new BadRequestException("Отсутствует колонка: " + header);
        }
    }

    private static String required(CSVRecord record, String field) {
        String value = nullable(record, field);
        if (value == null) throw new BadRequestException("Не заполнено поле " + field);
        return value;
    }

    private static String nullable(CSVRecord record, String field) {
        String value = record.get(field);
        if (value == null) return null;
        value = value.replace("\ufeff", "").trim();
        return value.isEmpty() ? null : value;
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new BadRequestException("Дата должна иметь формат YYYY-MM-DD"); }
    }

    private static BigDecimal parseGrade(String value) {
        if (value == null) return null;
        try { return new BigDecimal(value.replace(',', '.')); }
        catch (NumberFormatException ex) { throw new BadRequestException("Некорректный averageGrade"); }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "да" -> true;
            case "false", "0", "no", "нет" -> false;
            default -> throw new BadRequestException("hasDebt принимает true/false или да/нет");
        };
    }

    private static StudentStatus parseStatus(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "enrolled", "обучается" -> StudentStatus.ENROLLED;
            case "academic_leave", "академ", "академический_отпуск" -> StudentStatus.ACADEMIC_LEAVE;
            case "expelled", "отчислен" -> StudentStatus.EXPELLED;
            case "graduated", "выпускник" -> StudentStatus.GRADUATED;
            case "transferred", "переведен", "переведён" -> StudentStatus.TRANSFERRED;
            default -> throw new BadRequestException("Неизвестный status: " + value);
        };
    }

    private static Set<String> parseTags(String value) {
        if (value == null) return Set.of();
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        Arrays.stream(value.split("\\|")).map(String::trim).filter(v -> !v.isEmpty())
                .map(v -> v.toLowerCase(Locale.ROOT)).forEach(tags::add);
        return tags;
    }

    private static String safeCell(Object value) {
        if (value == null) return "";
        String cell = String.valueOf(value);
        if (!cell.isEmpty() && "=+-@\t\r".indexOf(cell.charAt(0)) >= 0) return "'" + cell;
        return cell;
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Некорректная строка" : message;
    }

    private record ParsedRow(long rowNumber, StudentRequest request) { }
    private record ParsedFile(int totalRows, List<ParsedRow> rows, List<RowIssue> issues) { }
}
