package ru.mirea.ispteam;

import ru.mirea.ispteam.repository.ConnectionRequestRepository;
import ru.mirea.ispteam.repository.ConnectionRequestRepositoryImpl;
import ru.mirea.ispteam.repository.SubscriberRepository;
import ru.mirea.ispteam.repository.SubscriberRepositoryImpl;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.service.ExportService;
import ru.mirea.ispteam.service.StatisticsService;
import ru.mirea.ispteam.service.SubscriberService;
import ru.mirea.ispteam.util.CsvExporter;
import ru.mirea.ispteam.util.DatabaseManager;
import ru.mirea.ispteam.util.ExcelExporter;

/*
 * Composition root: все объекты приложения создаются здесь и связываются через конструкторы —
 * снизу вверх по слоям util -> repository -> service. Интерфейс (JavaFX) получает готовые сервисы
 * и ничего не знает о БД и репозиториях.
 */
public final class AppContext {

    private final SubscriberService subscriberService;
    private final ConnectionRequestService requestService;
    private final StatisticsService statisticsService;
    private final ExportService exportService;

    private AppContext(DatabaseManager db) {
        // repository [A]
        SubscriberRepository subscriberRepository = new SubscriberRepositoryImpl(db);
        ConnectionRequestRepository requestRepository = new ConnectionRequestRepositoryImpl(db);

        // service [B, D]
        subscriberService = new SubscriberService(subscriberRepository, requestRepository);
        requestService = new ConnectionRequestService(requestRepository, subscriberRepository);
        statisticsService = new StatisticsService(subscriberService, requestService);
        exportService = new ExportService(subscriberService, requestService, new CsvExporter(), new ExcelExporter());
    }

    /**
     * Читает настройки БД, проверяет подключение и (если включено) применяет schema.sql / seed-data.sql.
     *
     * @throws ru.mirea.ispteam.exception.DatabaseException если БД недоступна или настройки не найдены
     */
    public static AppContext create() {
        DatabaseManager db = new DatabaseManager();
        db.checkConnection();
        db.initializeIfEnabled();
        return new AppContext(db);
    }

    public SubscriberService subscriberService() {
        return subscriberService;
    }

    public ConnectionRequestService requestService() {
        return requestService;
    }

    public StatisticsService statisticsService() {
        return statisticsService;
    }

    public ExportService exportService() {
        return exportService;
    }
}
