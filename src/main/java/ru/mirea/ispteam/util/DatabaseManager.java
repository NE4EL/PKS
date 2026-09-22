package ru.mirea.ispteam.util;

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
 * Конфигурация читается из classpath: src/main/resources/db.properties.
 */
public class DatabaseManager {

    private final String url;
    private final String user;
    private final String password;
    private final boolean initOnStartup;

    public DatabaseManager() {
        Properties props = loadProperties("db.properties");
        this.url = props.getProperty("db.url");
        this.user = props.getProperty("db.user");
        this.password = props.getProperty("db.password");
        this.initOnStartup = Boolean.parseBoolean(props.getProperty("db.initOnStartup", "false"));
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
     * Инициализация БД при старте приложения (если включена в db.properties):
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
            System.out.println("[DatabaseManager] БД инициализирована тестовыми данными (seed-data.sql).");
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

    private Properties loadProperties(String resourceName) {
        try (InputStream in = getResource(resourceName)) {
            Properties props = new Properties();
            props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            return props;
        } catch (IOException e) {
            throw new DatabaseException("Не удалось прочитать " + resourceName, e);
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
