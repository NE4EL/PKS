package ru.mirea.studenthub.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Печатает безопасную памятку с адресами локального подключения после полной
 * готовности Spring Boot.
 *
 * <p>В журнал выводятся только URL, имя БД и пользователь PostgreSQL. Пароли, JWT secret
 * и временный пароль администратора в журнал не записываются.</p>
 *
 * <p>Компонент отключён для профиля {@code prod}; публичный адрес рабочей среды
 * определяется настройками reverse proxy.</p>
 */
@Component
@Profile("!prod")
public final class StartupConnectionInfoLogger {

    private static final Logger log = LoggerFactory.getLogger(StartupConnectionInfoLogger.class);

    private final int appPort;
    private final int postgresPort;
    private final String postgresDatabase;
    private final String postgresUser;

    /**
     * Получает именно внешние dev-порты из environment Docker Compose.
     *
     * @param appPort внешний HTTP-порт приложения на машине разработчика
     * @param postgresPort внешний loopback-порт PostgreSQL
     * @param postgresDatabase имя локальной базы данных
     * @param postgresUser имя пользователя PostgreSQL
     */
    public StartupConnectionInfoLogger(
            @Value("${APP_PORT:8080}") int appPort,
            @Value("${POSTGRES_PORT:5432}") int postgresPort,
            @Value("${POSTGRES_DB:student_hub}") String postgresDatabase,
            @Value("${POSTGRES_USER:student_hub_app}") String postgresUser) {
        this.appPort = appPort;
        this.postgresPort = postgresPort;
        this.postgresDatabase = postgresDatabase;
        this.postgresUser = postgresUser;
    }

    /**
     * Вызывается только после {@link ApplicationReadyEvent}, то есть когда Spring
     * закончил инициализацию и приложение уже готово принимать HTTP-запросы.
     * Такой момент полезнее обычного сообщения в {@code main}: пользователь не
     * получает ложное "готово", пока Flyway, JPA или web server ещё запускаются.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void printConnectionInfo() {
        String base = "http://localhost:" + appPort;
        String postgresJdbc = "jdbc:postgresql://localhost:" + postgresPort + "/" + postgresDatabase;

        log.info("""

                ============================================================
                 STUDENT HUB IS READY
                ============================================================
                 Web UI:           {}/
                 Swagger UI:       {}/swagger-ui.html
                 REST API:         {}/api
                 OpenAPI JSON:     {}/v3/api-docs
                 Health:           {}/actuator/health
                 JavaFX API:       {}/api
                 Android emulator: http://10.0.2.2:{}/api
                 PostgreSQL JDBC:  {}
                 PostgreSQL user:  {}
                 Passwords/secrets are not printed.
                ============================================================
                """,
                base, base, base, base, base, base, appPort, postgresJdbc, postgresUser);
    }
}
