package ru.mirea.isp.service;

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
import ru.mirea.isp.config.AppProperties;
import ru.mirea.isp.domain.ConnectionRequest;
import ru.mirea.isp.domain.ConnectionRequestStatus;
import ru.mirea.isp.domain.Subscriber;
import ru.mirea.isp.domain.UserAccount;
import ru.mirea.isp.dto.ImportDtos.ImportMode;
import ru.mirea.isp.dto.ImportDtos.ImportPreview;
import ru.mirea.isp.dto.ImportDtos.ImportResult;
import ru.mirea.isp.dto.ImportDtos.RowIssue;
import ru.mirea.isp.dto.ConnectionRequestDtos.ConnectionRequestRequest;
import ru.mirea.isp.dto.ConnectionRequestDtos.ConnectionRequestResponse;
import ru.mirea.isp.error.BadRequestException;
import ru.mirea.isp.filter.SmartFilterParser;
import ru.mirea.isp.filter.ConnectionRequestSpecifications;
import ru.mirea.isp.repository.ConnectionRequestRepository;
import ru.mirea.isp.repository.SubscriberRepository;
import ru.mirea.isp.security.CurrentUserService;

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
public class CsvConnectionRequestService {
    private static final String[] HEADERS = {
            "externalId", "description", "title", "address", "email", "phone",
            "dueDate", "subscriber", "status", "type", "tariffPlan", "estimatedHours", "urgent", "tags"
    };

    private final ConnectionRequestRepository requests;
    private final SubscriberRepository subscribers;
    private final ConnectionRequestService requestService;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final ConnectionRequestMapper mapper;
    private final Validator validator;
    private final AppProperties properties;
    private final org.springframework.transaction.support.TransactionTemplate rowTransaction;
    private final AuditService audit;

    public CsvConnectionRequestService(ConnectionRequestRepository requests, SubscriberRepository subscribers,
                             ConnectionRequestService requestService, CurrentUserService currentUser,
                             SmartFilterParser parser, ConnectionRequestMapper mapper, Validator validator,
                             AppProperties properties, AuditService audit, org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.requests = requests;
        this.subscribers = subscribers;
        this.requestService = requestService;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.validator = validator;
        this.properties = properties;
        this.audit = audit;
        this.rowTransaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.rowTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Проверяет CSV без записи в базу. */
    @Transactional(readOnly = true)
    public ImportPreview preview(MultipartFile file, ImportMode mode) {
        ParsedFile parsed = parse(file, mode == null ? ImportMode.CREATE_ONLY : mode);
        return new ImportPreview(parsed.totalRows(), parsed.rows().size(), (int) parsed.issues().stream().map(RowIssue::row).distinct().count(), parsed.issues());
    }

    /** Применяет только валидные строки и возвращает детальный итог. */
    @Transactional
    public ImportResult apply(MultipartFile file, ImportMode mode) {
        ImportMode effectiveMode = mode == null ? ImportMode.CREATE_ONLY : mode;
        ParsedFile parsed = parse(file, effectiveMode);
        UserAccount actor = currentUser.requireEntity();
        int created = 0;
        int updated = 0;
        int skipped = (int) parsed.issues().stream().map(RowIssue::row).distinct().count();
        List<RowIssue> runtimeIssues = new ArrayList<>(parsed.issues());

        for (ParsedRow row : parsed.rows()) {
            try {
                Boolean isNew = rowTransaction.execute(tx -> {
                    ConnectionRequest existing = requests.findByExternalIdIgnoreCase(row.request().externalId()).orElse(null);
                    if (existing != null && effectiveMode == ImportMode.CREATE_ONLY) {
                        throw new BadRequestException("Запись уже существует");
                    }
                    requestService.upsertFromImport(existing, row.request(), actor);
                    requests.flush();
                    audit.record("REQUEST_CSV_ROW_IMPORTED", "REQUEST", null, "Импортирована строка CSV", Map.of("row",row.rowNumber()));
                    return existing == null;
                });
                if (Boolean.TRUE.equals(isNew)) created++; else updated++;
            } catch (RuntimeException ex) {
                runtimeIssues.add(new RowIssue(row.rowNumber(), "row", ex instanceof BadRequestException ? ex.getMessage() : "Не удалось применить строку"));
                skipped++;
            }
        }
        audit.record("REQUEST_CSV_IMPORTED", "REQUEST", null, "Импортирован CSV со заявками",
                Map.of("created", created, "updated", updated, "skipped", skipped, "mode", effectiveMode.name()));
        return new ImportResult(parsed.totalRows(), created, updated, skipped, List.copyOf(runtimeIssues));
    }

    /** Потоково экспортирует текущую выборку без загрузки всего набора в память. */
    @Transactional
    public void export(String query, OutputStream output) throws IOException {
        // BOM записывается до создания CSVPrinter, иначе заголовок окажется перед BOM.
        output.write("\ufeff".getBytes(StandardCharsets.UTF_8));
        try (Writer writer = new OutputStreamWriter(output, StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).get())) {
            int page = 0;
            while (true) {
                var result = requests.findAll(ConnectionRequestSpecifications.from(parser.parse(query)),
                        PageRequest.of(page, 500, Sort.by("description").ascending().and(Sort.by("title").ascending())));
                for (ConnectionRequest request : result.getContent()) {
                    ConnectionRequestResponse dto = mapper.toResponse(request);
                    printer.printRecord(
                            safeCell(dto.externalId()), safeCell(dto.description()), safeCell(dto.title()),
                            safeCell(dto.address()), safeCell(dto.email()), safeCell(dto.phone()),
                            dto.dueDate(), safeCell(dto.subscriberCode()), dto.status(), dto.type(), safeCell(dto.tariffPlan()), dto.estimatedHours(),
                            dto.urgent(), safeCell(String.join("|", dto.tags())));
                }
                if (result.isLast()) break;
                page++;
            }
            printer.flush();
        }
        audit.record("REQUEST_CSV_EXPORTED", "REQUEST", null, "Экспортирована выборка заявок", Map.of("query", query == null ? "" : query));
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
        try (Reader reader = bomReader(file);
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
                    Set<ConstraintViolation<ConnectionRequestRequest>> violations = validator.validate(row.request());
                    if (!violations.isEmpty()) {
                        for (ConstraintViolation<ConnectionRequestRequest> violation : violations) {
                            issues.add(new RowIssue(rowNumber, violation.getPropertyPath().toString(), violation.getMessage()));
                        }
                        continue;
                    }
                    String key = row.request().externalId().toLowerCase(Locale.ROOT);
                    if (!externalIdsInFile.add(key)) {
                        issues.add(new RowIssue(rowNumber, "externalId", "Дубликат externalId внутри файла"));
                        continue;
                    }
                    if (mode == ImportMode.CREATE_ONLY && requests.existsByExternalIdIgnoreCase(row.request().externalId())) {
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

    private static Reader bomReader(MultipartFile file) throws IOException {
        java.io.PushbackReader reader = new java.io.PushbackReader(new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8)), 1);
        int first = reader.read();
        if (first != -1 && first != 0xfeff) reader.unread(first);
        return reader;
    }

    private ParsedRow toRow(CSVRecord record, long rowNumber) {
        String externalId = required(record, "externalId");
        String subscriberCode = nullable(record, "subscriber");
        Subscriber subscriber = subscriberCode == null ? null : subscribers.findByCodeIgnoreCase(subscriberCode)
                .orElseThrow(() -> new BadRequestException("Абонент не найден: " + subscriberCode));
        if (subscriber != null && !subscriber.isActive()) throw new BadRequestException("Абонент неактивна: " + subscriberCode);

        ConnectionRequestRequest request = new ConnectionRequestRequest(
                externalId,
                required(record, "title"),
                required(record, "description"),
                nullable(record, "address"),
                nullable(record, "email"),
                nullable(record, "phone"),
                parseDate(nullable(record, "dueDate")),
                subscriber == null ? null : subscriber.getId(),
                parseStatus(required(record, "status")),
                ru.mirea.isp.domain.RequestType.valueOf(required(record, "type").toUpperCase(Locale.ROOT)),
                nullable(record, "tariffPlan"),
                parseHours(nullable(record, "estimatedHours")),
                parseBoolean(nullable(record, "urgent")),
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
        // Undo only our CSV formula escape; doubling a literal apostrophe preserves it.
        if (value.length() > 1 && value.charAt(0) == '\'' && "'=+-@\t\r".indexOf(value.charAt(1)) >= 0) {
            value = value.substring(1);
        }
        return value.isEmpty() ? null : value;
    }

    private static LocalDate parseDate(String value) {
        if (value == null) return null;
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException ex) { throw new BadRequestException("Дата должна иметь формат YYYY-MM-DD"); }
    }

    private static BigDecimal parseHours(String value) {
        if (value == null) return null;
        try { return new BigDecimal(value.replace(',', '.')); }
        catch (NumberFormatException ex) { throw new BadRequestException("Некорректный estimatedHours"); }
    }

    private static boolean parseBoolean(String value) {
        if (value == null) return false;
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "true", "1", "yes", "да" -> true;
            case "false", "0", "no", "нет" -> false;
            default -> throw new BadRequestException("urgent принимает true/false или да/нет");
        };
    }

    private static ConnectionRequestStatus parseStatus(String value) {
        String normalized = value.toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try { return ConnectionRequestStatus.valueOf(normalized.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BadRequestException("Неизвестный статус: " + value); }
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
        if (!cell.isEmpty() && "'=+-@\t\r".indexOf(cell.charAt(0)) >= 0) return "'" + cell;
        return cell;
    }

    private static String safeMessage(RuntimeException ex) {
        String message = ex.getMessage();
        return message == null || message.isBlank() ? "Некорректная строка" : message;
    }

    private record ParsedRow(long rowNumber, ConnectionRequestRequest request) { }
    private record ParsedFile(int totalRows, List<ParsedRow> rows, List<RowIssue> issues) { }
}
