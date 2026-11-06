package ru.mirea.ispteam.fx.model;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;

import java.time.LocalDateTime;

/**
 * Строка таблицы заявок: заявка + связанный абонент.
 * В таблице вместо технического subscriberId показываются ФИО и телефон абонента.
 */
public record RequestRow(ConnectionRequest request, Subscriber subscriber) {

    public Long id() {
        return request.getId();
    }

    public String subscriberName() {
        return subscriber != null ? subscriber.getFullName() : "(абонент удалён)";
    }

    public String subscriberPhone() {
        return subscriber != null ? subscriber.getPhone() : "";
    }

    public RequestType type() {
        return request.getType();
    }

    public RequestStatus status() {
        return request.getStatus();
    }

    public String tariffPlan() {
        return request.getTariffPlan();
    }

    public String description() {
        return request.getDescription();
    }

    public LocalDateTime createdAt() {
        return request.getCreatedAt();
    }

    public LocalDateTime updatedAt() {
        return request.getUpdatedAt();
    }
}
