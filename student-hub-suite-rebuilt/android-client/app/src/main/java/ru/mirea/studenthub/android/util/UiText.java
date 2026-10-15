package ru.mirea.studenthub.android.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

import ru.mirea.studenthub.android.model.ApiModels;

/**
 * Единая локализация технических enum-кодов backend для Android UI.
 *
 * <p>JSON enum-коды остаются без изменений, а русские названия задаются только
 * на уровне интерфейса.</p>
 */
public final class UiText {
    private UiText() {
    }

    public static String role(ApiModels.Role role) {
        if (role == null) return "—";
        return switch (role) {
            case ADMIN -> "Администратор";
            case TEACHER -> "Преподаватель";
            case VIEWER -> "Наблюдатель";
        };
    }

    public static String roles(Collection<ApiModels.Role> roles) {
        if (roles == null || roles.isEmpty()) return "—";
        return roles.stream().map(UiText::role).collect(Collectors.joining(", "));
    }

    public static String status(ApiModels.StudentStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case ENROLLED -> "Обучается";
            case ACADEMIC_LEAVE -> "Академический отпуск";
            case EXPELLED -> "Отчислен";
            case GRADUATED -> "Выпускник";
            case TRANSFERRED -> "Переведён";
        };
    }

    public static String risk(ApiModels.RiskLevel risk) {
        if (risk == null) return "—";
        return switch (risk) {
            case NOT_APPLICABLE -> "Не применяется";
            case LOW -> "Низкий";
            case MEDIUM -> "Средний";
            case HIGH -> "Высокий";
            case CRITICAL -> "Критический";
        };
    }

    /** Форматирует балл без лишних нулей после целого значения. */
    public static String grade(BigDecimal grade) {
        if (grade == null) return "—";
        BigDecimal normalized = grade.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros();
        return normalized.toPlainString();
    }

    public static String yesNo(boolean value) {
        return value ? "Да" : "Нет";
    }

    public static String group(ApiModels.Group group) {
        if (group == null) return "Без группы";
        StringBuilder result = new StringBuilder(group.code == null ? "" : group.code);
        if (group.name != null && !group.name.trim().isEmpty()) result.append(" — ").append(group.name);
        return result.toString();
    }

    public static String auditEvent(String eventType) {
        if (eventType == null) return "—";
        return switch (eventType) {
            case "LOGIN_SUCCESS" -> "Успешный вход";
            case "LOGIN_FAILED" -> "Неудачный вход";
            case "LOGOUT" -> "Выход";
            case "PASSWORD_CHANGED" -> "Смена пароля";
            case "INITIAL_PASSWORD_CHANGED" -> "Установка постоянного пароля";
            case "STUDENT_CREATED" -> "Создание студента";
            case "STUDENT_UPDATED" -> "Изменение студента";
            case "STUDENT_ARCHIVED" -> "Архивирование студента";
            case "STUDENT_RESTORED" -> "Восстановление студента";
            case "USER_CREATED" -> "Создание пользователя";
            case "USER_UPDATED" -> "Изменение пользователя";
            case "USER_PASSWORD_RESET" -> "Сброс пароля пользователя";
            default -> eventType.toLowerCase(Locale.ROOT).replace('_', ' ');
        };
    }
}
