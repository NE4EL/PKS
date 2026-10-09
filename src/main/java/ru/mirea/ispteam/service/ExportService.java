package ru.mirea.ispteam.service;

import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.util.CsvExporter;
import ru.mirea.ispteam.util.ExcelExporter;
import ru.mirea.ispteam.util.TableData;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * КР2 (D). Экспорт текущего набора данных в .csv или .xlsx.
 * Сервис готовит строки (ФИО абонента вместо его ID, русские названия статусов),
 * а запись файла выполняют util-классы CsvExporter / ExcelExporter.
 * Путь к файлу выбирает пользователь (FileChooser в JavaFX) — здесь он только передаётся.
 */
public class ExportService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final SubscriberService subscriberService;
    private final ConnectionRequestService requestService;
    private final CsvExporter csvExporter;
    private final ExcelExporter excelExporter;

    public ExportService(SubscriberService subscriberService, ConnectionRequestService requestService,
                         CsvExporter csvExporter, ExcelExporter excelExporter) {
        this.subscriberService = subscriberService;
        this.requestService = requestService;
        this.csvExporter = csvExporter;
        this.excelExporter = excelExporter;
    }

    public void exportRequests(List<ConnectionRequest> requests, Path file) {
        write(requestsTable(requests), file);
    }

    public void exportSubscribers(List<Subscriber> subscribers, Path file) {
        write(subscribersTable(subscribers), file);
    }

    TableData requestsTable(List<ConnectionRequest> requests) {
        Map<Long, Subscriber> subscribers = subscriberService.getAll().stream()
                .collect(Collectors.toMap(Subscriber::getId, Function.identity()));
        List<List<String>> rows = new ArrayList<>();
        for (ConnectionRequest r : requests) {
            Subscriber s = subscribers.get(r.getSubscriberId());
            rows.add(List.of(
                    String.valueOf(r.getId()),
                    s != null ? s.getFullName() : "",
                    s != null ? s.getPhone() : "",
                    r.getType().getTitle(),
                    r.getStatus().getTitle(),
                    text(r.getTariffPlan()),
                    text(r.getDescription()),
                    r.getCreatedAt() != null ? r.getCreatedAt().format(DATE_TIME) : "",
                    r.getUpdatedAt() != null ? r.getUpdatedAt().format(DATE_TIME) : ""));
        }
        return new TableData("Заявки", List.of("№ заявки", "Абонент", "Телефон", "Тип", "Статус",
                "Тариф", "Описание", "Создана", "Обновлена"), rows);
    }

    TableData subscribersTable(List<Subscriber> subscribers) {
        Map<Long, List<ConnectionRequest>> requestsBySubscriber = requestService.getAll().stream()
                .collect(Collectors.groupingBy(ConnectionRequest::getSubscriberId));
        List<List<String>> rows = new ArrayList<>();
        for (Subscriber s : subscribers) {
            List<ConnectionRequest> own = requestsBySubscriber.getOrDefault(s.getId(), List.of());
            rows.add(List.of(
                    s.getFullName(),
                    s.getPhone(),
                    text(s.getEmail()),
                    s.getAddress(),
                    s.getRegistrationDate() != null ? s.getRegistrationDate().format(DATE) : "",
                    String.valueOf(own.size()),
                    String.valueOf(own.stream().filter(r -> r.getStatus().isActive()).count())));
        }
        return new TableData("Абоненты", List.of("ФИО", "Телефон", "Email", "Адрес",
                "Дата регистрации", "Заявок всего", "Активных заявок"), rows);
    }

    private void write(TableData data, Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".csv")) {
            csvExporter.export(data, file);
        } else if (name.endsWith(".xlsx")) {
            excelExporter.export(data, file);
        } else {
            throw new ValidationException("file", "Поддерживаются только форматы .csv и .xlsx");
        }
    }

    private static String text(String value) {
        return value != null ? value : "";
    }
}
