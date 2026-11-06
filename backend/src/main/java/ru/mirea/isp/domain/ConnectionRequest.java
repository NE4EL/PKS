package ru.mirea.isp.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Основная доменная сущность заявки.
 *
 * <p>Обычное удаление реализовано через архивирование, поэтому история и связанные
 * записи остаются доступными для восстановления.</p>
 */
@Entity
@Table(name = "request")
public class ConnectionRequest extends AuditedVersionedEntity {
    @Id
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true, length = 100)
    private String externalId;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "description", nullable = false, length = 1000)
    private String description;

    @Column(name = "address", length = 500)
    private String address;

    @Column(length = 254)
    private String email;

    @Column(length = 32)
    private String phone;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscriber_id")
    private Subscriber subscriber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RequestType type = RequestType.NEW_CONNECTION;
    @Column(name = "tariff_plan", length = 100)
    private String tariffPlan;
    public RequestType getType() { return type; }
    public void setType(RequestType type) { this.type = type; }
    public String getTariffPlan() { return tariffPlan; }
    public void setTariffPlan(String tariffPlan) { this.tariffPlan = tariffPlan; }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ConnectionRequestStatus status = ConnectionRequestStatus.NEW;

    @Column(name = "estimated_hours", precision = 5, scale = 2)
    private BigDecimal estimatedHours;

    @Column(name = "urgent", nullable = false)
    private boolean urgent;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority_level", nullable = false, length = 32)
    private PriorityLevel priorityLevel = PriorityLevel.LOW;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "archived_by")
    private UserAccount archivedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserAccount createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserAccount updatedBy;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "request_tag", joinColumns = @JoinColumn(name = "request_id"))
    @Column(name = "tag", nullable = false, length = 50)
    private Set<String> tags = new LinkedHashSet<>();

    protected ConnectionRequest() {
    }

    public ConnectionRequest(UUID id, String title, String description, UserAccount creator) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.createdBy = creator;
        this.updatedBy = creator;
    }

    public UUID getId() { return id; }
    public String getExternalId() { return externalId; }
    public void setExternalId(String externalId) { this.externalId = externalId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public Subscriber getSubscriber() { return subscriber; }
    public void setSubscriber(Subscriber subscriber) { this.subscriber = subscriber; }
    public ConnectionRequestStatus getStatus() { return status; }
    public void setStatus(ConnectionRequestStatus status) { this.status = status; }
    public BigDecimal getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(BigDecimal estimatedHours) { this.estimatedHours = estimatedHours; }
    public boolean isUrgent() { return urgent; }
    public void setUrgent(boolean urgent) { this.urgent = urgent; }
    public PriorityLevel getPriorityLevel() { return priorityLevel; }
    public void setPriorityLevel(PriorityLevel priorityLevel) { this.priorityLevel = priorityLevel; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public Instant getArchivedAt() { return archivedAt; }
    public void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
    public UserAccount getArchivedBy() { return archivedBy; }
    public void setArchivedBy(UserAccount archivedBy) { this.archivedBy = archivedBy; }
    public UserAccount getCreatedBy() { return createdBy; }
    public UserAccount getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UserAccount updatedBy) { this.updatedBy = updatedBy; }
    public Set<String> getTags() { return tags; }
    public void setTags(Set<String> tags) { this.tags = new LinkedHashSet<>(tags); }
}
