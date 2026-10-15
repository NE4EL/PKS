package ru.mirea.studenthub.android.util;

/**
 * Чистая Java-логика телефонной маски +7-999-999-99-99.
 *
 * <p>Класс не зависит от Android и покрывается unit-тестами. Любой ввод очищается от
 * букв и знаков, 8XXXXXXXXXX/7XXXXXXXXXX нормализуются к российскому +7.</p>
 */
public final class PhoneFormatter {
    private PhoneFormatter() {
    }

    public static String format(String value) {
        String digits = value == null ? "" : value.replaceAll("\\D", "");

        if (digits.startsWith("8")) {
            digits = "7" + digits.substring(1);
        }
        if (digits.startsWith("7")) {
            digits = digits.substring(1);
        }
        if (digits.length() > 10) {
            digits = digits.substring(0, 10);
        }

        StringBuilder result = new StringBuilder("+7-");
        if (digits.isEmpty()) return result.toString();

        appendPart(result, digits, 0, 3);
        if (digits.length() > 3) appendPartWithDash(result, digits, 3, 6);
        if (digits.length() > 6) appendPartWithDash(result, digits, 6, 8);
        if (digits.length() > 8) appendPartWithDash(result, digits, 8, 10);
        return result.toString();
    }

    public static boolean isComplete(String value) {
        return value != null && value.matches("^\\+7-\\d{3}-\\d{3}-\\d{2}-\\d{2}$");
    }

    private static void appendPart(StringBuilder out, String digits, int start, int end) {
        out.append(digits, start, Math.min(end, digits.length()));
    }

    private static void appendPartWithDash(StringBuilder out, String digits, int start, int end) {
        out.append('-');
        appendPart(out, digits, start, end);
    }
}
