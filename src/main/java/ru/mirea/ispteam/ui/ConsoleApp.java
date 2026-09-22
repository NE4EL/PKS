package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.service.ConnectionRequestQueryService;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.service.StatisticsService;
import ru.mirea.ispteam.service.SubscriberService;

/*
 * Владелец: C (UI). Скелет создан A.
 *
 * Зависимости (сервисы B и D) проброшены в конструктор — UI работает только через них,
 * SQL и Connection здесь недопустимы (PKS.md разд. 4, участник C).
 *
 * TODO(C): реализовать главное меню (PKS.md разд. 1) и цикл выбора пунктов,
 * делегируя в SubscriberMenu / RequestMenu / StatisticsMenu.
 */
public class ConsoleApp {

    private final SubscriberService subscriberService;
    private final ConnectionRequestService requestService;
    private final ConnectionRequestQueryService queryService;
    private final StatisticsService statisticsService;

    public ConsoleApp(SubscriberService subscriberService,
                      ConnectionRequestService requestService,
                      ConnectionRequestQueryService queryService,
                      StatisticsService statisticsService) {
        this.subscriberService = subscriberService;
        this.requestService = requestService;
        this.queryService = queryService;
        this.statisticsService = statisticsService;
    }

    public void run() {
        printMainMenu();
        System.out.println();
        System.out.println(">>> Каркас проекта собран и запущен. UI ещё не реализован (участник C).");
        System.out.println(">>> Слой БД/репозиториев (участник A) готов к использованию сервисами.");
    }

    // Главное меню — эталон из PKS.md разд. 1. TODO(C): сделать интерактивным.
    private void printMainMenu() {
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
}
