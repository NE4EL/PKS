package ru.mirea.ispteam.exception;

/*
 * Владелец: A.
 * Оборачивает низкоуровневые SQLException в непроверяемое доменное исключение,
 * чтобы верхние слои не зависели от java.sql.
 */
public class DatabaseException extends RuntimeException {

    public DatabaseException(String message) {
        super(message);
    }

    public DatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
