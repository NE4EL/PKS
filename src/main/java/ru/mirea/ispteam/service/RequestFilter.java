package ru.mirea.ispteam.service;

import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;

import java.time.LocalDate;
import java.util.Locale;

/**
 * КР2 (D). Набор условий поиска и фильтрации заявок. Любое условие может быть null — тогда оно не применяется.
 *
 * @param query  подстрока поиска по ФИО и телефону абонента, описанию и тарифу (без учёта регистра)
 * @param status фильтр по статусу
 * @param type   фильтр по типу заявки
 * @param from   начало периода по дате создания (включительно)
 * @param to     конец периода по дате создания (включительно)
 */
public record RequestFilter(String query, RequestStatus status, RequestType type,
                            LocalDate from, LocalDate to) {

    public static final RequestFilter EMPTY = new RequestFilter(null, null, null, null, null);

    /** Проверяет логическую корректность периода: дата «с» не позже даты «по». */
    public void validate() {
        if (from != null && to != null && from.isAfter(to)) {
            throw new ValidationException("period", "Дата «с» не может быть позже даты «по»");
        }
    }

    /**
     * Подходит ли заявка под все заданные условия.
     *
     * @param subscriber абонент заявки (нужен для поиска по ФИО/телефону), может быть null
     */
    public boolean matches(ConnectionRequest request, Subscriber subscriber) {
        if (status != null && request.getStatus() != status) return false;
        if (type != null && request.getType() != type) return false;
        if (from != null || to != null) {
            if (request.getCreatedAt() == null) return false;
            LocalDate created = request.getCreatedAt().toLocalDate();
            if (from != null && created.isBefore(from)) return false;
            if (to != null && created.isAfter(to)) return false;
        }
        return matchesQuery(request, subscriber);
    }

    private boolean matchesQuery(ConnectionRequest request, Subscriber subscriber) {
        if (query == null || query.isBlank()) return true;
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return contains(request.getDescription(), needle)
                || contains(request.getTariffPlan(), needle)
                || (subscriber != null && (contains(subscriber.getFullName(), needle)
                        || contains(subscriber.getPhone(), needle)));
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
