package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.Subscriber;

import java.util.Locale;

/**
 * КР2 (D). Условия поиска и фильтрации абонентов.
 *
 * @param query          подстрока поиска по ФИО, телефону, email и адресу (без учёта регистра)
 * @param onlyWithActive показывать только абонентов, у которых есть активные заявки
 */
public record SubscriberFilter(String query, boolean onlyWithActive) {

    /**
     * @param activeRequests количество активных заявок абонента
     */
    public boolean matches(Subscriber subscriber, long activeRequests) {
        if (onlyWithActive && activeRequests == 0) return false;
        if (query == null || query.isBlank()) return true;
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return contains(subscriber.getFullName(), needle)
                || contains(subscriber.getPhone(), needle)
                || contains(subscriber.getEmail(), needle)
                || contains(subscriber.getAddress(), needle);
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }
}
