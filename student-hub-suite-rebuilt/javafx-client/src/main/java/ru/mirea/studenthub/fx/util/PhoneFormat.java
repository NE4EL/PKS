package ru.mirea.studenthub.fx.util;

/**
 * Нормализует российский телефон в единый формат {@code +7-999-999-99-99}.
 *
 * <p>Форматирование вынесено из JavaFX-контрола, поэтому его можно тестировать без
 * запуска JavaFX toolkit. Номер, начинающийся с 8 или 7, приводится к +7.</p>
 */
public final class PhoneFormat {
    private PhoneFormat() { }

    /** Возвращает форматированное значение либо пустую строку. */
    public static String normalize(String source) {
        if (source == null || source.isBlank()) return "";

        String digits = source.replaceAll("\\D", "");

        if (digits.startsWith("8")) {
            digits = digits.substring(1);
        } else if (digits.startsWith("7")) {
            digits = digits.substring(1);
        }

        if (digits.length() > 10) digits = digits.substring(0, 10);

        StringBuilder result = new StringBuilder("+7-");
        append(result, digits, 0, 3);
        if (digits.length() > 3) {
            result.append('-');
            append(result, digits, 3, 6);
        }
        if (digits.length() > 6) {
            result.append('-');
            append(result, digits, 6, 8);
        }
        if (digits.length() > 8) {
            result.append('-');
            append(result, digits, 8, 10);
        }
        return result.toString();
    }

    /** Проверяет полностью заполненный номер. */
    public static boolean isComplete(String value) {
        return value != null && value.matches("^\\+7-\\d{3}-\\d{3}-\\d{2}-\\d{2}$");
    }

    private static void append(StringBuilder target, String digits, int from, int to) {
        if (digits.length() <= from) return;
        target.append(digits, from, Math.min(to, digits.length()));
    }
}
