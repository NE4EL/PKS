package ru.mirea.ispteam.exception;

/*
 * КР2. Ошибка при выгрузке данных в файл (.csv / .xlsx): нет прав на запись,
 * файл открыт в другой программе и т.п. Оборачивает IOException.
 */
public class ExportException extends RuntimeException {

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
