package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.service.ConnectionRequestQueryService;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.service.SubscriberService;
import ru.mirea.ispteam.util.InputReader;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Владелец: C (UI).
 * Подменю "Заявки" (CRUD + смена статуса), а также пункты главного меню
 * "Поиск" и "Фильтрация и сортировка" — все они выводят таблицу заявок.
 * Бизнес-правила (переходы статусов, длина описания, одна активная заявка на подключение)
 * проверяет ConnectionRequestService (B), поиск/фильтры — ConnectionRequestQueryService (D).
 */
public class RequestMenu {

    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final String[] TABLE_HEADERS =
            {"ID", "Абонент", "Тип", "Статус", "Тариф", "Описание", "Создана", "Обновлена"};

    private final ConnectionRequestService requestService;
    private final ConnectionRequestQueryService queryService;
    private final SubscriberService subscriberService;
    private final InputReader input;

    public RequestMenu(ConnectionRequestService requestService,
                       ConnectionRequestQueryService queryService,
                       SubscriberService subscriberService,
                       InputReader input) {
        this.requestService = requestService;
        this.queryService = queryService;
        this.subscriberService = subscriberService;
        this.input = input;
    }

    // ---------- 2. Заявки ----------

    public void show() {
        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.printHeader("ЗАЯВКИ");
            System.out.println("1. Список заявок");
            System.out.println("2. Создать заявку");
            System.out.println("3. Изменить заявку");
            System.out.println("4. Изменить статус заявки");
            System.out.println("5. Удалить заявку");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите пункт: ", 0, 5);
            if (choice == 0) {
                return;
            }
            ConsoleHelper.runSafely(() -> {
                switch (choice) {
                    case 1 -> printAll();
                    case 2 -> create();
                    case 3 -> edit();
                    case 4 -> changeStatus();
                    case 5 -> delete();
                    default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
                }
            });
            input.waitForEnter();
        }
    }

    public void printAll() {
        ConsoleHelper.printHeader("ТАБЛИЦА: ЗАЯВКИ");
        printTable(requestService.getAll());
    }

    private void create() {
        ConsoleHelper.printHeader("НОВАЯ ЗАЯВКА");
        long subscriberId = input.readLong("ID абонента: ");
        RequestType type = input.readEnum("Тип заявки:", RequestType.values(), Labels.types());
        String tariffPlan = needsTariff(type) ? input.readOptional("Тарифный план: ") : null;
        String description = input.readNonEmptyString("Описание: ");

        LocalDateTime now = LocalDateTime.now();
        ConnectionRequest created = requestService.create(new ConnectionRequest(
                subscriberId, type, RequestStatus.NEW, tariffPlan, description, now, now));
        ConsoleHelper.printSuccess("Заявка создана, ID = " + created.getId());
    }

    private void edit() {
        long id = input.readLong("ID заявки: ");
        ConnectionRequest current = requestService.getById(id);
        printTable(List.of(current));

        System.out.println("Введите новые значения (Enter — оставить как есть):");
        String description = input.readOrKeep("Описание", current.getDescription());
        String tariffPlan = needsTariff(current.getType())
                ? input.readOrKeep("Тарифный план", current.getTariffPlan())
                : current.getTariffPlan();

        ConnectionRequest updated = new ConnectionRequest(id, current.getSubscriberId(),
                current.getType(), current.getStatus(), tariffPlan, description,
                current.getCreatedAt(), LocalDateTime.now());
        requestService.update(id, updated);
        ConsoleHelper.printSuccess("Заявка ID = " + id + " обновлена");
    }

    private void changeStatus() {
        long id = input.readLong("ID заявки: ");
        ConnectionRequest current = requestService.getById(id);
        printTable(List.of(current));

        // Допустимость перехода проверяет сервис (бизнес-правило 3) — UI её не дублирует.
        RequestStatus newStatus = input.readEnum("Новый статус:", RequestStatus.values(), Labels.statuses());
        requestService.changeStatus(id, newStatus);
        ConsoleHelper.printSuccess("Статус заявки ID = " + id + ": "
                + Labels.status(current.getStatus()) + " -> " + Labels.status(newStatus));
    }

    private void delete() {
        long id = input.readLong("ID заявки: ");
        ConnectionRequest current = requestService.getById(id);
        printTable(List.of(current));

        if (!input.readYesNo("Удалить заявку ID = " + id + "?")) {
            System.out.println("Удаление отменено.");
            return;
        }
        requestService.delete(id);
        ConsoleHelper.printSuccess("Заявка ID = " + id + " удалена");
    }

    // ---------- 3. Поиск ----------

    public void showSearch() {
        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.printHeader("ПОИСК ЗАЯВОК");
            System.out.println("1. По абоненту (ФИО или телефон)");
            System.out.println("2. По ключевому слову в описании");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите пункт: ", 0, 2);
            if (choice == 0) {
                return;
            }
            ConsoleHelper.runSafely(() -> {
                switch (choice) {
                    case 1 -> printTable(queryService.searchBySubscriber(
                            input.readNonEmptyString("ФИО или телефон (можно часть): ")));
                    case 2 -> printTable(queryService.searchByDescription(
                            input.readNonEmptyString("Ключевое слово: ")));
                    default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
                }
            });
            input.waitForEnter();
        }
    }

    // ---------- 4. Фильтрация и сортировка ----------

    public void showFilterAndSort() {
        while (true) {
            ConsoleHelper.clearScreen();
            ConsoleHelper.printHeader("ФИЛЬТРАЦИЯ И СОРТИРОВКА");
            System.out.println("1. Фильтр по статусу");
            System.out.println("2. Фильтр по типу");
            System.out.println("3. Сортировка по дате создания (сначала новые)");
            System.out.println("4. Сортировка по дате создания (сначала старые)");
            System.out.println("5. Сортировка по статусу");
            System.out.println("0. Назад");
            int choice = input.readInt("Выберите пункт: ", 0, 5);
            if (choice == 0) {
                return;
            }
            ConsoleHelper.runSafely(() -> {
                switch (choice) {
                    case 1 -> printTable(queryService.filterByStatus(
                            input.readEnum("Статус:", RequestStatus.values(), Labels.statuses())));
                    case 2 -> printTable(queryService.filterByType(
                            input.readEnum("Тип:", RequestType.values(), Labels.types())));
                    case 3 -> printTable(queryService.sortByCreatedAt(false));
                    case 4 -> printTable(queryService.sortByCreatedAt(true));
                    case 5 -> printTable(queryService.sortByStatus());
                    default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
                }
            });
            input.waitForEnter();
        }
    }

    // ---------- вывод ----------

    private void printTable(List<ConnectionRequest> requests) {
        Map<Long, String> names = subscriberNames();
        List<String[]> rows = new ArrayList<>();
        for (ConnectionRequest r : requests) {
            rows.add(new String[]{
                    String.valueOf(r.getId()),
                    names.getOrDefault(r.getSubscriberId(), "ID " + r.getSubscriberId()),
                    Labels.type(r.getType()),
                    Labels.status(r.getStatus()),
                    r.getTariffPlan(),
                    r.getDescription(),
                    format(r.getCreatedAt()),
                    format(r.getUpdatedAt())
            });
        }
        TablePrinter.print(TABLE_HEADERS, rows);
    }

    /*
     * id абонента -> "Фамилия И. О." для колонки "Абонент".
     * Имена — только украшение: если получить абонентов не удалось,
     * таблица всё равно выводится (с ID вместо имени).
     */
    private Map<Long, String> subscriberNames() {
        Map<Long, String> names = new HashMap<>();
        try {
            for (Subscriber s : subscriberService.getAll()) {
                names.put(s.getId(), shortName(s.getFullName()) + " (" + s.getId() + ")");
            }
        } catch (RuntimeException e) {
            // оставляем пустую карту — в колонке будет ID абонента
        }
        return names;
    }

    // "Иванов Иван Иванович" -> "Иванов И. И."
    private static String shortName(String fullName) {
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(i == 1 ? " " : "").append(parts[i].charAt(0)).append('.');
            if (i < parts.length - 1) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    private static boolean needsTariff(RequestType type) {
        return type == RequestType.NEW_CONNECTION || type == RequestType.TARIFF_CHANGE;
    }

    private static String format(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATE_TIME_FORMAT) : null;
    }
}
