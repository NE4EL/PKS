package ru.mirea.studenthub.android.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Транспортные DTO Android-клиента.
 *
 * <p>Сетевой контракт использует отдельные DTO и не зависит от JPA/Room-сущностей.
 * Даты передаются как ISO-строки в формате, определённом серверным API.</p>
 */
public final class ApiModels {
    private ApiModels() {
    }

    /** Серверные коды ролей остаются английскими; UI локализует их отдельно. */
    public enum Role { VIEWER, TEACHER, ADMIN }

    public enum StudentStatus {
        ENROLLED,
        ACADEMIC_LEAVE,
        EXPELLED,
        GRADUATED,
        TRANSFERRED
    }

    /** NOT_APPLICABLE используется для отчисленных, выпускников и переведённых. */
    public enum RiskLevel {
        NOT_APPLICABLE,
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public enum BulkAction { ARCHIVE, RESTORE, SET_STATUS, ADD_TAG, REMOVE_TAG }
    public enum ImportMode { CREATE_ONLY, UPSERT_BY_EXTERNAL_ID }

    /** Native login нужен, чтобы backend вернул refresh token в JSON, а не cookie. */
    public static final class LoginRequest {
        public String email;
        public String password;
        public String clientType = "NATIVE";

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    public static final class RefreshRequest {
        public String refreshToken;

        public RefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }
    }

    public static final class ChangePasswordRequest {
        public String currentPassword;
        public String newPassword;

        public ChangePasswordRequest(String currentPassword, String newPassword) {
            this.currentPassword = currentPassword;
            this.newPassword = newPassword;
        }
    }

    /** Первичная смена временного пароля не требует повторной передачи старого секрета. */
    public static final class InitialPasswordChangeRequest {
        public String newPassword;

        public InitialPasswordChangeRequest(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    public static final class CurrentUser {
        public String id;
        public String email;
        public String displayName;
        public Set<Role> roles;
        public boolean passwordChangeRequired;

        public boolean isAdmin() {
            return roles != null && roles.contains(Role.ADMIN);
        }

        public boolean canEdit() {
            return isAdmin() || (roles != null && roles.contains(Role.TEACHER));
        }
    }

    public static final class AuthResponse {
        public String accessToken;
        public String accessExpiresAt;
        public String refreshToken;
        public String refreshExpiresAt;
        public CurrentUser user;
    }

    public static final class PageResponse<T> {
        public List<T> content;
        public int page;
        public int size;
        public long totalElements;
        public int totalPages;
        public boolean first;
        public boolean last;
    }

    public static final class Student {
        public String id;
        public String externalId;
        public String firstName;
        public String lastName;
        public String middleName;
        public String fullName;
        public String email;
        public String phone;
        public String birthDate;
        public String groupId;
        public String groupCode;
        public StudentStatus status;
        public BigDecimal averageGrade;
        public boolean hasDebt;
        public RiskLevel riskLevel;
        public boolean archived;
        public String archivedAt;
        public Set<String> tags;
        public String createdAt;
        public String updatedAt;
        public long version;
    }

    public static final class StudentRequest {
        public String externalId;
        public String firstName;
        public String lastName;
        public String middleName;
        public String email;
        public String phone;
        public String birthDate;
        public String groupId;
        public StudentStatus status;
        public BigDecimal averageGrade;
        public boolean hasDebt;
        public Set<String> tags;
        public Long version;
    }

    public static final class BulkRequest {
        public Set<String> ids;
        public BulkAction action;
        public StudentStatus status;
        public String tag;
    }

    public static final class BulkFailure {
        public String id;
        public String message;
    }

    public static final class BulkResult {
        public int requested;
        public int succeeded;
        public List<BulkFailure> failures;
    }

    public static final class Group {
        public String id;
        public String code;
        public String name;
        public Integer admissionYear;
        public boolean active;
        public String createdAt;
        public String updatedAt;
        public long version;

        @Override
        public String toString() {
            return code == null ? "" : code;
        }
    }

    public static final class GroupRequest {
        public String code;
        public String name;
        public Integer admissionYear;
        public Boolean active;
        public Long version;
    }

    public static final class Note {
        public String id;
        public String studentId;
        public String authorId;
        public String authorName;
        public String body;
        public boolean pinned;
        public String createdAt;
        public String updatedAt;
        public long version;
    }

    public static final class NoteRequest {
        public String body;
        public boolean pinned;
        public Long version;

        public NoteRequest(String body, boolean pinned, Long version) {
            this.body = body;
            this.pinned = pinned;
            this.version = version;
        }
    }

    public static final class User {
        public String id;
        public String email;
        public String displayName;
        public boolean enabled;
        public boolean passwordChangeRequired;
        public Set<Role> roles;
        public String lockedUntil;
        public String createdAt;
        public String updatedAt;
        public long version;
    }

    public static final class CreateUserRequest {
        public String email;
        public String displayName;
        public String temporaryPassword;
        public Set<Role> roles;
    }

    public static final class UpdateUserRequest {
        public String email;
        public String displayName;
        public boolean enabled;
        public Set<Role> roles;
        public long version;
    }

    public static final class ResetPasswordRequest {
        public String temporaryPassword;

        public ResetPasswordRequest(String temporaryPassword) {
            this.temporaryPassword = temporaryPassword;
        }
    }

    public static final class NamedCount {
        public String name;
        public long count;
    }

    public static final class Analytics {
        public long total;
        public long active;
        public long archived;
        public long withDebt;
        public BigDecimal averageGrade;
        public List<NamedCount> byGroup;
        public List<NamedCount> byStatus;
        public List<NamedCount> byRisk;
    }

    public static final class Audit {
        public String id;
        public String actorId;
        public String actorEmail;
        public String eventType;
        public String entityType;
        public String entityId;
        public String summary;
        public String correlationId;
        public String ipAddress;
        public String occurredAt;
        public String detailsJson;
    }

    public static final class RowIssue {
        public long row;
        public String field;
        public String message;
    }

    public static final class ImportPreview {
        public int totalRows;
        public int validRows;
        public int invalidRows;
        public List<RowIssue> issues;
    }

    public static final class ImportResult {
        public int totalRows;
        public int created;
        public int updated;
        public int skipped;
        public List<RowIssue> issues;
    }

    public static final class ApiError {
        public String code;
        public String message;
        public String correlationId;
        public Map<String, String> validationErrors;
    }
}
