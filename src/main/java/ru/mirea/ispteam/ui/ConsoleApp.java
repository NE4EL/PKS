package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.service.ConnectionRequestQueryService;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.service.StatisticsService;
import ru.mirea.ispteam.service.SubscriberService;
import ru.mirea.ispteam.util.ExcelExporter;
import ru.mirea.ispteam.util.InputReader;

/*
 * Владелец: C (UI).
 *
 * Главное меню приложения (PKS.md разд. 1). Само ничего не считает и не хранит —
 * делегирует в подменю, а те вызывают сервисы B и D.
 * SQL и Connection здесь недопустимы (PKS.md разд. 4, участник C).
 */
public class ConsoleApp {

    private static final String DEFAULT_EXPORT_FILE = "requests.xlsx";

    private final ConnectionRequestService requestService;
    private final ExcelExporter excelExporter;
    private final InputReader input = new InputReader();

    private final SubscriberMenu subscriberMenu;
    private final RequestMenu requestMenu;
    private final StatisticsMenu statisticsMenu;

    public ConsoleApp(SubscriberService subscriberService,
                      ConnectionRequestService requestService,
                      ConnectionRequestQueryService queryService,
                      StatisticsService statisticsService,
                      ExcelExporter excelExporter) {
        this.requestService = requestService;
        this.excelExporter = excelExporter;
        this.subscriberMenu = new SubscriberMenu(subscriberService, input);
        this.requestMenu = new RequestMenu(requestService, queryService, subscriberService, input);
        this.statisticsMenu = new StatisticsMenu(statisticsService, input);
    }

    public void run() {
        ConsoleHelper.enterAlternateScreen();
        // Ctrl+C не доходит до finally — JVM завершается сразу, но перед этим выполняет
        // shutdown hook'и. Через hook возвращаем обычный экран и в этом случае.
        Runtime.getRuntime().addShutdownHook(new Thread(ConsoleHelper::exitAlternateScreen));
        try {
            menuLoop();
        } finally {
            ConsoleHelper.exitAlternateScreen();
        }
        System.out.println("До свидания!");
    }

    private void menuLoop() {
        try {
            while (true) {
                ConsoleHelper.clearScreen();
                printMainMenu();
                int choice = input.readInt("Выберите пункт: ", 0, 7);
                if (choice == 0) {
                    break;
                }
                boolean ok = ConsoleHelper.runSafely(() -> handle(choice));
                // Подменю сами ждут Enter после своих действий; здесь пауза нужна,
                // чтобы пользователь успел прочитать ошибку или итог экспорта до очистки экрана.
                if (!ok || choice == 6) {
                    input.waitForEnter();
                }
            }
        } catch (InputReader.InputClosedException e) {
            System.out.println();
        }
    }

    private void handle(int choice) {
        switch (choice) {
            case 1 -> subscriberMenu.show();
            case 2 -> requestMenu.show();
            case 3 -> requestMenu.showSearch();
            case 4 -> requestMenu.showFilterAndSort();
            case 5 -> statisticsMenu.show();
            case 6 -> exportData();
            case 7 -> printDatabaseTables();
            default -> throw new IllegalStateException("Неизвестный пункт меню: " + choice);
        }
    }

    // Главное меню — эталон из PKS.md разд. 1.
    private void printMainMenu() {
        System.out.println();
        System.out.println("СИСТЕМА УЧЁТА ЗАЯВОК ИНТЕРНЕТ-ПРОВАЙДЕРА");
        System.out.println("========================================");
        System.out.println("1. Абоненты");
        System.out.println("2. Заявки");
        System.out.println("3. Поиск");
        System.out.println("4. Фильтрация и сортировка");
        System.out.println("5. Статистика");
        System.out.println("6. Экспорт данных");
        System.out.println("7. Вывести таблицы базы данных");
        System.out.println("0. Выход");
    }

    private void exportData() {
        ConsoleHelper.clearScreen();
        ConsoleHelper.printHeader("ЭКСПОРТ ЗАЯВОК В EXCEL");
        String path = input.readLine("Имя файла [" + DEFAULT_EXPORT_FILE + "]: ");
        if (path.isEmpty()) {
            path = DEFAULT_EXPORT_FILE;
        } else if (!path.toLowerCase().endsWith(".xlsx")) {
            path += ".xlsx";
        }
        excelExporter.exportRequests(requestService.getAll(), path);
        ConsoleHelper.printSuccess("Заявки выгружены в файл " + path);
    }

    // Обе таблицы БД целиком — данные берутся через сервисы, не через SQL.
    private void printDatabaseTables() {
        ConsoleHelper.clearScreen();
        subscriberMenu.printAll();
        requestMenu.printAll();
        input.waitForEnter();
    }
}
