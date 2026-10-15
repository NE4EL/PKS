package ru.mirea.studenthub.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ru.mirea.studenthub.domain.RiskLevel;
import ru.mirea.studenthub.domain.StudentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO-контракты студентов и массовых операций. */
public final class StudentDtos {
    private StudentDtos() {
    }

    /** Входная модель создания и изменения карточки. */
    public record StudentRequest(
            @Size(max = 100) String externalId,
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 100) String middleName,
            @Email @Size(max = 254) String email,
            @Pattern(regexp = "^\\+7-\\d{3}-\\d{3}-\\d{2}-\\d{2}$", message = "некорректный телефон") String phone,
            @Past LocalDate birthDate,
            UUID groupId,
            @NotNull StudentStatus status,
            @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal averageGrade,
            boolean hasDebt,
            @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags,
            Long version) {
    }

    /** Выходная карточка студента. */
    public record StudentResponse(
            UUID id,
            String externalId,
            String firstName,
            String lastName,
            String middleName,
            String fullName,
            String email,
            String phone,
            LocalDate birthDate,
            UUID groupId,
            String groupCode,
            StudentStatus status,
            BigDecimal averageGrade,
            boolean hasDebt,
            RiskLevel riskLevel,
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
            @NotNull @Size(min = 1, max = 500) Set<UUID> ids,
            @NotNull BulkAction action,
            StudentStatus status,
            @Size(max = 50) String tag) {
    }

    /** Результат массовой операции с явным списком ошибок. */
    public record BulkResult(int requested, int succeeded, List<BulkFailure> failures) {
    }

    /** Ошибка отдельной записи в массовой операции. */
    public record BulkFailure(UUID id, String message) {
    }
}
