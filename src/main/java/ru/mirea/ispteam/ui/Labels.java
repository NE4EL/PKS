package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;

/*
 * Владелец: C (UI).
 * Русские подписи для enum'ов — только для отображения. В БД и сервисах
 * по-прежнему хранятся имена констант (NEW, IN_PROGRESS, ...).
 */
final class Labels {

    private Labels() {
    }

    static String status(RequestStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case NEW -> "Новая";
            case IN_PROGRESS -> "В работе";
            case APPROVED -> "Одобрена";
            case COMPLETED -> "Выполнена";
            case REJECTED -> "Отклонена";
            case CANCELLED -> "Отменена";
        };
    }

    static String type(RequestType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case NEW_CONNECTION -> "Подключение";
            case TARIFF_CHANGE -> "Смена тарифа";
            case TECHNICAL_SUPPORT -> "Техподдержка";
            case DISCONNECTION -> "Отключение";
            case EQUIPMENT_REPLACEMENT -> "Замена оборудования";
        };
    }

    static String[] statuses() {
        RequestStatus[] values = RequestStatus.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = status(values[i]);
        }
        return labels;
    }

    static String[] types() {
        RequestType[] values = RequestType.values();
        String[] labels = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            labels[i] = type(values[i]);
        }
        return labels;
    }
}
