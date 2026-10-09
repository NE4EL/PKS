package ru.mirea.ispteam.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.ispteam.exception.DatabaseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/*
 * Владелец: A.
 * Отвечает за подключение к PostgreSQL через JDBC и (опционально) инициализацию БД:
 * применение schema.sql при старте и seed-data.sql — только если БД пустая.
 *
 * Конфигурация читается из classpath: src/main/resources/database.properties.
 * Файл не коммитится (.gitignore) — пароль БД не хранится в исходном коде.
 * Любой параметр можно переопределить системным свойством, например: -Ddb.password=...
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String CONFIG_FILE = "database.properties";

    private final String url;
    private final String user;
    private final String password;
    private final boolean initOnStartup;

    public DatabaseManager() {
        Properties props = loadConfig();
        this.url = setting(props, "db.url", null);
        this.user = setting(props, "db.user", null);
        this.password = setting(props, "db.password", null);
        this.initOnStartup = Boolean.parseBoolean(setting(props, "db.initOnStartup", "false"));
        if (url == null || url.isBlank()) {
            throw new DatabaseException("В database.properties не задан параметр db.url");
        }
    }

    /**
     * Новое соединение с БД. Вызывающий обязан закрыть его (try-with-resources).
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось подключиться к БД: " + url, e);
        }
    }

    /**
     * Проверка при старте приложения: открывает и сразу закрывает соединение.
     * Так ошибка в настройках видна сразу, а не при первом действии пользователя.
     */
    public void checkConnection() {
        try (Connection ignored = getConnection()) {
            log.info("Подключение к БД установлено: {}", url);
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка при проверке подключения к БД: " + url, e);
        }
    }

    /**
     * Инициализация БД при старте приложения (если включена в database.properties):
     *  - всегда применяет schema.sql (таблицы создаются через IF NOT EXISTS);
     *  - применяет seed-data.sql только если таблица subscribers пустая.
     */
    public void initializeIfEnabled() {
        if (!initOnStartup) {
            return;
        }
        runScript("schema.sql");
        if (isDatabaseEmpty()) {
            runScript("seed-data.sql");
            log.info("БД инициализирована тестовыми данными (seed-data.sql)");
        }
    }

    private boolean isDatabaseEmpty() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM subscribers")) {
            return rs.next() && rs.getLong(1) == 0;
        } catch (SQLException e) {
            throw new DatabaseException("Не удалось проверить наполнение БД", e);
        }
    }

    /**
     * Выполняет SQL-скрипт из classpath. Скрипт разбивается на инструкции по ';'.
     */
    public void runScript(String resourceName) {
        String script = readResource(resourceName);
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {
            for (String statement : script.split(";")) {
                String sql = stripSqlComments(statement).trim();
                if (!sql.isEmpty()) {
                    st.execute(sql);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Ошибка выполнения скрипта " + resourceName, e);
        }
    }

    private String stripSqlComments(String sql) {
        StringBuilder sb = new StringBuilder();
        for (String line : sql.split("\n")) {
            int comment = line.indexOf("--");
            sb.append(comment >= 0 ? line.substring(0, comment) : line).append('\n');
        }
        return sb.toString();
    }

    private static String setting(Properties props, String key, String defaultValue) {
        return System.getProperty(key, props.getProperty(key, defaultValue));
    }

    private Properties loadConfig() {
        InputStream source = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE);
        if (source == null) {
            throw new DatabaseException("Не найден файл настроек src/main/resources/database.properties. "
                    + "Скопируйте database.properties.example в database.properties и укажите свои параметры БД.");
        }
        try (InputStream in = source) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return props;
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать " + CONFIG_FILE, e);
        }
    }

    private String readResource(String resourceName) {
        try (InputStream in = getResource(resourceName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
            return sb.toString();
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать ресурс " + resourceName, e);
        }
    }

    private InputStream getResource(String resourceName) {
        InputStream in = getClass().getClassLoader().getResourceAsStream(resourceName);
        if (in == null) {
            throw new DatabaseException("Ресурс не найден в classpath: " + resourceName);
        }
        return in;
    }
}
