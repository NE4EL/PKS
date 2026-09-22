package ru.mirea.ispteam;

/*
 * Владелец: C (точка входа).
 *
 * TODO(C): реализовать composition root — собрать граф зависимостей и запустить ConsoleApp.
 *
 * Порядок сборки слоёв (каждый слой получает зависимости через конструктор):
 *   1) util:       DatabaseManager db = new DatabaseManager(); db.initializeIfEnabled();
 *   2) repository:  SubscriberRepositoryImpl(db), ConnectionRequestRepositoryImpl(db)   [готово, A]
 *   3) service:     SubscriberService(...), ConnectionRequestService(...)               [B]
 *                   ConnectionRequestQueryService(...), StatisticsService(...)          [D]
 *   4) ui:          new ConsoleApp(subscriberService, requestService,
 *                                  queryService, statisticsService).run();
 *
 * Конструкторы всех классов уже объявлены — осталось связать их здесь.
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("TODO(C): точка входа не реализована — см. Main.java");
    }
}
