package ru.mirea.studenthub.fx.net;

/** Ошибка REST-вызова, которую можно показать пользователю. */
public class ApiException extends RuntimeException {
    private final int status;
    private final String correlationId;

    public ApiException(int status, String message, String correlationId) {
        super(message);
        this.status = status;
        this.correlationId = correlationId;
    }

    public int getStatus() { return status; }
    public String getCorrelationId() { return correlationId; }
}
