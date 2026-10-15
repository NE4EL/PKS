package ru.mirea.studenthub.filter;

import org.springframework.stereotype.Component;
import ru.mirea.studenthub.domain.RiskLevel;
import ru.mirea.studenthub.domain.StudentStatus;
import ru.mirea.studenthub.error.BadRequestException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбирает человекочитаемый фильтр в типобезопасную модель.
 *
 * <p>Исходная строка фильтра не объединяется с SQL/JPQL. Парсер принимает только
 * известные команды, а Criteria API передаёт значения через параметры.</p>
 */
@Component
public class SmartFilterParser {
    private static final Pattern TOKEN = Pattern.compile("\\\"([^\\\"]*)\\\"|(\\S+)");
    private static final Pattern GRADE = Pattern.compile("^(?:балл|grade|gpa)(<=|>=|=|<|>)([0-9]+(?:[.,][0-9]{1,2})?)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Разбирает строку; по умолчанию исключает архивные записи. */
    public StudentFilter parse(String source) {
        List<String> free = new ArrayList<>();
        String group = null;
        StudentStatus status = null;
        Boolean debt = null;
        RiskLevel risk = null;
        String tag = null;
        Boolean archived = false;
        List<StudentFilter.GradeConstraint> grades = new ArrayList<>();

        if (source == null || source.isBlank()) {
            return new StudentFilter("", null, null, null, null, null, false, List.of());
        }
        if (source.length() > 500) throw new BadRequestException("Строка фильтра слишком длинная");

        Matcher matcher = TOKEN.matcher(source.trim());
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String token = raw.trim();
            if (token.isEmpty()) continue;
            String lower = token.toLowerCase(Locale.ROOT);

            Matcher gradeMatcher = GRADE.matcher(lower);
            if (gradeMatcher.matches()) {
                BigDecimal value = new BigDecimal(gradeMatcher.group(2).replace(',', '.'));
                if (value.compareTo(new BigDecimal("2.00")) < 0 || value.compareTo(new BigDecimal("5.00")) > 0) {
                    throw new BadRequestException("Значение среднего балла должно быть от 2 до 5");
                }
                grades.add(new StudentFilter.GradeConstraint(operator(gradeMatcher.group(1)), value));
                continue;
            }

            int colon = token.indexOf(':');
            if (colon > 0) {
                String key = lower.substring(0, colon);
                String value = stripQuotes(token.substring(colon + 1).trim());
                String normalized = value.toLowerCase(Locale.ROOT);
                switch (key) {
                    case "группа", "group" -> group = value;
                    case "статус", "status" -> status = status(normalized);
                    case "долг", "debt" -> debt = booleanValue(normalized, "долг");
                    case "риск", "risk" -> risk = risk(normalized);
                    case "тег", "tag" -> tag = value;
                    case "архив", "archived" -> archived = archivedValue(normalized);
                    default -> free.add(token);
                }
            } else {
                free.add(token);
            }
        }
        return new StudentFilter(String.join(" ", free), blankToNull(group), status, debt, risk,
                blankToNull(tag), archived, List.copyOf(grades));
    }

    private static StudentFilter.Operator operator(String value) {
        return switch (value) {
            case "<" -> StudentFilter.Operator.LT;
            case "<=" -> StudentFilter.Operator.LTE;
            case "=" -> StudentFilter.Operator.EQ;
            case ">=" -> StudentFilter.Operator.GTE;
            case ">" -> StudentFilter.Operator.GT;
            default -> throw new BadRequestException("Неизвестный оператор среднего балла");
        };
    }

    private static StudentStatus status(String value) {
        return switch (value.replace('-', '_')) {
            case "обучается", "enrolled" -> StudentStatus.ENROLLED;
            case "академ", "академический_отпуск", "academic_leave" -> StudentStatus.ACADEMIC_LEAVE;
            case "отчислен", "expelled" -> StudentStatus.EXPELLED;
            case "выпускник", "graduated" -> StudentStatus.GRADUATED;
            case "переведен", "переведён", "transferred" -> StudentStatus.TRANSFERRED;
            default -> throw new BadRequestException("Неизвестный статус: " + value);
        };
    }

    private static RiskLevel risk(String value) {
        return switch (value) {
            case "не_применяется", "неприменимо", "not_applicable", "n/a" -> RiskLevel.NOT_APPLICABLE;
            case "низкий", "low" -> RiskLevel.LOW;
            case "средний", "medium" -> RiskLevel.MEDIUM;
            case "высокий", "high" -> RiskLevel.HIGH;
            case "критический", "critical" -> RiskLevel.CRITICAL;
            default -> throw new BadRequestException("Неизвестный риск: " + value);
        };
    }

    private static Boolean booleanValue(String value, String field) {
        return switch (value) {
            case "да", "yes", "true", "1" -> true;
            case "нет", "no", "false", "0" -> false;
            default -> throw new BadRequestException("Поле «" + field + "» принимает да/нет");
        };
    }

    private static Boolean archivedValue(String value) {
        if (value.equals("все") || value.equals("all")) return null;
        return booleanValue(value, "архив");
    }

    private static String stripQuotes(String value) {
        return value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")
                ? value.substring(1, value.length() - 1) : value;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
