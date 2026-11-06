package ru.mirea.isp.filter;

import org.springframework.stereotype.Component;
import ru.mirea.isp.domain.PriorityLevel;
import ru.mirea.isp.domain.ConnectionRequestStatus;
import ru.mirea.isp.error.BadRequestException;

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
    private static final Pattern HOURS = Pattern.compile("^(?:часы|hours)(<=|>=|=|<|>)([0-9]+(?:[.,][0-9]{1,2})?)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /** Разбирает строку; по умолчанию исключает архивные записи. */
    public ConnectionRequestFilter parse(String source) {
        List<String> free = new ArrayList<>();
        String subscriber = null;
        ConnectionRequestStatus status = null;
        Boolean urgent = null;
        PriorityLevel priority = null;
        String tag = null;
        Boolean archived = false;
        List<ConnectionRequestFilter.HoursConstraint> hours = new ArrayList<>();

        if (source == null || source.isBlank()) {
            return new ConnectionRequestFilter("", null, null, null, null, null, false, List.of());
        }
        if (source.length() > 500) throw new BadRequestException("Строка фильтра слишком длинная");

        Matcher matcher = TOKEN.matcher(source.trim());
        while (matcher.find()) {
            String raw = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
            String token = raw.trim();
            if (token.isEmpty()) continue;
            String lower = token.toLowerCase(Locale.ROOT);

            Matcher hoursMatcher = HOURS.matcher(lower);
            if (hoursMatcher.matches()) {
                BigDecimal value = new BigDecimal(hoursMatcher.group(2).replace(',', '.'));
                if (value.compareTo(new BigDecimal("0.00")) < 0 || value.compareTo(new BigDecimal("100.00")) > 0) {
                    throw new BadRequestException("Значение трудозатрат должно быть от 0 до 100");
                }
                hours.add(new ConnectionRequestFilter.HoursConstraint(operator(hoursMatcher.group(1)), value));
                continue;
            }

            int colon = token.indexOf(':');
            if (colon > 0) {
                String key = lower.substring(0, colon);
                String value = stripQuotes(token.substring(colon + 1).trim());
                String normalized = value.toLowerCase(Locale.ROOT);
                switch (key) {
                    case "абонент", "subscriber" -> subscriber = value;
                    case "статус", "status" -> status = status(normalized);
                    case "срочно", "urgent" -> urgent = booleanValue(normalized, "срочно");
                    case "приоритет", "priority" -> priority = priority(normalized);
                    case "тег", "tag" -> tag = value;
                    case "архив", "archived" -> archived = archivedValue(normalized);
                    default -> free.add(token);
                }
            } else {
                free.add(token);
            }
        }
        return new ConnectionRequestFilter(String.join(" ", free), blankToNull(subscriber), status, urgent, priority,
                blankToNull(tag), archived, List.copyOf(hours));
    }

    private static ConnectionRequestFilter.Operator operator(String value) {
        return switch (value) {
            case "<" -> ConnectionRequestFilter.Operator.LT;
            case "<=" -> ConnectionRequestFilter.Operator.LTE;
            case "=" -> ConnectionRequestFilter.Operator.EQ;
            case ">=" -> ConnectionRequestFilter.Operator.GTE;
            case ">" -> ConnectionRequestFilter.Operator.GT;
            default -> throw new BadRequestException("Неизвестный оператор трудозатрат");
        };
    }

    private static ConnectionRequestStatus status(String value) {
        try { return ConnectionRequestStatus.valueOf(value.replace('-', '_').toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { throw new BadRequestException("Неизвестный статус: " + value); }
    }

    private static PriorityLevel priority(String value) {
        return switch (value) {
            case "не_применяется", "неприменимо", "not_applicable", "n/a" -> PriorityLevel.NOT_APPLICABLE;
            case "низкий", "low" -> PriorityLevel.LOW;
            case "средний", "medium" -> PriorityLevel.MEDIUM;
            case "высокий", "high" -> PriorityLevel.HIGH;
            case "критический", "critical" -> PriorityLevel.CRITICAL;
            default -> throw new BadRequestException("Неизвестный приоритет: " + value);
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
