package ru.mirea.studenthub.fx.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * DTO desktop-клиента, повторяющие публичный REST-контракт backend.
 *
 * <p>Модели клиента содержат только поля публичного REST-контракта и не зависят
 * от серверных JPA-сущностей.</p>
 */
public final class Models {
    private Models() { }

    /**
     * Технические коды ролей остаются английскими для REST/JWT,
     * но JavaFX отображает пользователю русские названия через toString().
     */
    public enum Role {
        VIEWER("Наблюдатель"),
        TEACHER("Преподаватель"),
        ADMIN("Администратор");

        private final String label;
        Role(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    /** Статусы студента с локализованным представлением. */
    public enum StudentStatus {
        ENROLLED("Обучается"),
        ACADEMIC_LEAVE("Академический отпуск"),
        EXPELLED("Отчислен"),
        GRADUATED("Выпускник"),
        TRANSFERRED("Переведён");

        private final String label;
        StudentStatus(String label) { this.label = label; }
        @Override public String toString() { return label; }

        public static String labelOf(String raw) {
            if (raw == null) return "—";
            try { return valueOf(raw).toString(); }
            catch (IllegalArgumentException ignored) { return raw; }
        }
    }

    /** Уровень академического риска. */
    public enum RiskLevel {
        NOT_APPLICABLE("Не применяется"),
        LOW("Низкий"),
        MEDIUM("Средний"),
        HIGH("Высокий"),
        CRITICAL("Критический");

        private final String label;
        RiskLevel(String label) { this.label = label; }
        @Override public String toString() { return label; }

        public static String labelOf(String raw) {
            if (raw == null) return "—";
            try { return valueOf(raw).toString(); }
            catch (IllegalArgumentException ignored) { return raw; }
        }
    }

    public enum ImportMode {
        CREATE_ONLY("Только новые записи"),
        UPSERT_BY_EXTERNAL_ID("Создать/обновить по External ID");

        private final String label;
        ImportMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    public enum BulkAction {
        ARCHIVE("В архив"),
        RESTORE("Восстановить"),
        SET_STATUS("Изменить статус"),
        ADD_TAG("Добавить тег"),
        REMOVE_TAG("Удалить тег");

        private final String label;
        BulkAction(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CurrentUser(UUID id, String email, String displayName, Set<Role> roles,
                              boolean passwordChangeRequired) {
        public boolean isAdmin() { return roles != null && roles.contains(Role.ADMIN); }
        public boolean canEdit() { return isAdmin() || roles != null && roles.contains(Role.TEACHER); }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AuthResponse(String accessToken, Instant accessExpiresAt, String refreshToken,
                               Instant refreshExpiresAt, CurrentUser user) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PageResponse<T>(List<T> content, int page, int size, long totalElements,
                                  int totalPages, boolean first, boolean last) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Student(UUID id, String externalId, String firstName, String lastName,
                          String middleName, String fullName, String email, String phone,
                          LocalDate birthDate, UUID groupId, String groupCode,
                          StudentStatus status, BigDecimal averageGrade, boolean hasDebt,
                          RiskLevel riskLevel, boolean archived, Instant archivedAt,
                          Set<String> tags, Instant createdAt, Instant updatedAt, long version) { }

    public record StudentRequest(String externalId, String firstName, String lastName,
                                 String middleName, String email, String phone,
                                 LocalDate birthDate, UUID groupId, StudentStatus status,
                                 BigDecimal averageGrade, boolean hasDebt, Set<String> tags,
                                 Long version) { }

    public record BulkRequest(Set<UUID> ids, BulkAction action, StudentStatus status, String tag) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BulkFailure(UUID id, String message) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BulkResult(int requested, int succeeded, List<BulkFailure> failures) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Group(UUID id, String code, String name, Integer admissionYear, boolean active,
                        Instant createdAt, Instant updatedAt, long version) {
        @Override public String toString() { return code; }
    }

    public record GroupRequest(String code, String name, Integer admissionYear, Boolean active, Long version) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Note(UUID id, UUID studentId, UUID authorId, String authorName, String body,
                       boolean pinned, Instant createdAt, Instant updatedAt, long version) { }

    public record NoteRequest(String body, boolean pinned, Long version) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record User(UUID id, String email, String displayName, boolean enabled,
                       boolean passwordChangeRequired, Set<Role> roles, Instant lockedUntil,
                       Instant createdAt, Instant updatedAt, long version) { }

    public record CreateUserRequest(String email, String displayName, String temporaryPassword,
                                    Set<Role> roles) { }
    public record UpdateUserRequest(String email, String displayName, boolean enabled,
                                    Set<Role> roles, long version) { }
    public record ResetPasswordRequest(String temporaryPassword) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NamedCount(String name, long count) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Analytics(long total, long active, long archived, long withDebt,
                            BigDecimal averageGrade, List<NamedCount> byGroup,
                            List<NamedCount> byStatus, List<NamedCount> byRisk) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Audit(UUID id, UUID actorId, String actorEmail, String eventType,
                        String entityType, UUID entityId, String summary,
                        String correlationId, String ipAddress, Instant occurredAt,
                        String detailsJson) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RowIssue(long row, String field, String message) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImportPreview(int totalRows, int validRows, int invalidRows,
                                List<RowIssue> issues) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImportResult(int totalRows, int created, int updated, int skipped,
                               List<RowIssue> issues) { }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ApiError(String code, String message, String correlationId,
                           java.util.Map<String, String> validationErrors) { }
}
