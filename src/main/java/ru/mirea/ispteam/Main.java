package ru.mirea.ispteam;

import ru.mirea.ispteam.exception.DatabaseException;
import ru.mirea.ispteam.repository.ConnectionRequestRepository;
import ru.mirea.ispteam.repository.ConnectionRequestRepositoryImpl;
import ru.mirea.ispteam.repository.SubscriberRepository;
import ru.mirea.ispteam.repository.SubscriberRepositoryImpl;
import ru.mirea.ispteam.service.ConnectionRequestQueryService;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.service.StatisticsService;
import ru.mirea.ispteam.service.SubscriberService;
import ru.mirea.ispteam.ui.ConsoleApp;
import ru.mirea.ispteam.util.DatabaseManager;
import ru.mirea.ispteam.util.ExcelExporter;

/*
 * Владелец: C (точка входа).
 *
 * Composition root: здесь (и только здесь) создаются все объекты приложения
 * и связываются через конструкторы — снизу вверх по слоям:
 *   util -> repository -> service -> ui.
 * В КР2 (JavaFX) поменяется только последний шаг — вместо ConsoleApp будет другой UI.
 */
public class Main {

    public static void main(String[] args) {
        try {
            // 1) util: подключение к БД (+ schema.sql / seed-data.sql, если включено)
            DatabaseManager db = new DatabaseManager();
            db.initializeIfEnabled();

            // 2) repository [A]
            SubscriberRepository subscriberRepository = new SubscriberRepositoryImpl(db);
            ConnectionRequestRepository requestRepository = new ConnectionRequestRepositoryImpl(db);

            // 3) service [B, D]
            SubscriberService subscriberService =
                    new SubscriberService(subscriberRepository, requestRepository);
            ConnectionRequestService requestService =
                    new ConnectionRequestService(requestRepository, subscriberRepository);
            ConnectionRequestQueryService queryService =
                    new ConnectionRequestQueryService(requestService, subscriberService);
            StatisticsService statisticsService =
                    new StatisticsService(subscriberService, requestService);

            // 4) ui [C]
            new ConsoleApp(subscriberService, requestService, queryService,
                    statisticsService, new ExcelExporter()).run();
        } catch (DatabaseException e) {
            System.err.println("Не удалось запустить приложение: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("Причина: " + e.getCause().getMessage());
            }
            System.err.println("Проверьте, что PostgreSQL запущен и настроен src/main/resources/db.properties.");
            System.exit(1);
        }
    }
}
