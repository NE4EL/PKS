package ru.mirea.ispteam;

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

/*
 * Владелец: C (точка входа). Скелет-«сборка» создан A.
 *
 * Composition root: здесь собирается граф зависимостей всех слоёв
 *   util (A) -> repository (A) -> service (B, D) -> ui (C).
 * Каждый слой получает зависимости через конструктор.
 */
public class Main {

    public static void main(String[] args) {
        // --- Слой инфраструктуры и данных (A) ---
        DatabaseManager db = new DatabaseManager();
        db.initializeIfEnabled(); // применит schema.sql и (при пустой БД) seed-data.sql

        SubscriberRepository subscriberRepository = new SubscriberRepositoryImpl(db);
        ConnectionRequestRepository requestRepository = new ConnectionRequestRepositoryImpl(db);

        // --- Слой бизнес-логики (B) ---
        SubscriberService subscriberService =
                new SubscriberService(subscriberRepository, requestRepository);
        ConnectionRequestService requestService =
                new ConnectionRequestService(requestRepository, subscriberRepository);

        // --- Слой запросов/статистики (D) ---
        ConnectionRequestQueryService queryService =
                new ConnectionRequestQueryService(requestService);
        StatisticsService statisticsService =
                new StatisticsService(subscriberService, requestService);

        // --- Слой UI (C) ---
        ConsoleApp app = new ConsoleApp(subscriberService, requestService, queryService, statisticsService);
        app.run();
    }
}
