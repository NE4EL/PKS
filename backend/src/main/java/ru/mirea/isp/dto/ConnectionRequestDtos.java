package ru.mirea.isp.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.mirea.isp.domain.PriorityLevel;
import ru.mirea.isp.domain.RequestType;
import ru.mirea.isp.domain.ConnectionRequestStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO-контракты заявок и массовых операций. */
public final class ConnectionRequestDtos {
    private ConnectionRequestDtos() {
    }

    /** Входная модель создания и изменения карточки. */
    public record ConnectionRequestRequest(
            @NotBlank @Size(max = 100) String externalId,
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 1000) String description,
            @Size(max = 500) String address,
            @Email @Size(max = 254) String email,
            @Pattern(regexp = "^\\+7-\\d{3}-\\d{3}-\\d{2}-\\d{2}$", message = "некорректный телефон") String phone,
            LocalDate dueDate,
            @NotNull UUID subscriberId,
            @NotNull ConnectionRequestStatus status,
            @NotNull RequestType type,
            @Size(max = 100) String tariffPlan,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal estimatedHours,
            boolean urgent,
            @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
            Long version) {
    }

    /** Выходная карточка заявки. */
    public record ConnectionRequestResponse(
            UUID id,
            String externalId,
            String title,
            String description,
            String address,
            String email,
            String phone,
            LocalDate dueDate,
            UUID subscriberId,
            String subscriberCode,
            ConnectionRequestStatus status,
            RequestType type,
            String tariffPlan,
            BigDecimal estimatedHours,
            boolean urgent,
            PriorityLevel priorityLevel,
            boolean archived,
            Instant archivedAt,
            Set<String> tags,
            Instant createdAt,
            Instant updatedAt,
            long version) {
    }

    /** Поддерживаемые массовые действия. */
    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }

    /** Запрос массового изменения. */
    public record BulkRequest(
            @NotNull @Size(min = 1, max = 500) Set<@NotNull UUID> ids,
            @NotNull BulkAction action,
            ConnectionRequestStatus status,
            @Size(max = 50) String tag) {
    }

    /** Результат массовой операции с явным списком ошибок. */
    public record BulkResult(int requested, int succeeded, List<BulkFailure> failures) {
    }

    /** Ошибка отдельной записи в массовой операции. */
    public record BulkFailure(UUID id, String message) {
    }
}
