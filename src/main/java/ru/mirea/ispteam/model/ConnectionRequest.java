package ru.mirea.ispteam.model;

import java.time.LocalDateTime;
import java.util.Objects;

/*
 * Владелец: D (модель). Скелет создан A (рабочий класс — нужен репозиториям).
 * Поля/типы — эталон из PKS.md разд. 2.2. Менять состав полей только по согласованию.
 */
public class ConnectionRequest {

    private Long id;
    private Long subscriberId;
    private RequestType type;
    private RequestStatus status;
    private String tariffPlan;   // nullable
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public ConnectionRequest() {
    }

    // Конструктор для создания новой заявки (без id — его назначит БД)
    public ConnectionRequest(Long subscriberId, RequestType type, RequestStatus status,
                             String tariffPlan, String description,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.subscriberId = subscriberId;
        this.type = type;
        this.status = status;
        this.tariffPlan = tariffPlan;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Полный конструктор (например, при чтении из БД)
    public ConnectionRequest(Long id, Long subscriberId, RequestType type, RequestStatus status,
                             String tariffPlan, String description,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.subscriberId = subscriberId;
        this.type = type;
        this.status = status;
        this.tariffPlan = tariffPlan;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubscriberId() {
        return subscriberId;
    }

    public void setSubscriberId(Long subscriberId) {
        this.subscriberId = subscriberId;
    }

    public RequestType getType() {
        return type;
    }

    public void setType(RequestType type) {
        this.type = type;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getTariffPlan() {
        return tariffPlan;
    }

    public void setTariffPlan(String tariffPlan) {
        this.tariffPlan = tariffPlan;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionRequest that = (ConnectionRequest) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "ConnectionRequest{" +
                "id=" + id +
                ", subscriberId=" + subscriberId +
                ", type=" + type +
                ", status=" + status +
                ", tariffPlan='" + tariffPlan + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
