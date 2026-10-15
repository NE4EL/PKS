package ru.mirea.studenthub.error;

/** Сигнал отсутствия запрошенной сущности. */
public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}
