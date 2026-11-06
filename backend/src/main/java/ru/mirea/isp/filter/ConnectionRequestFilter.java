package ru.mirea.isp.filter;

import ru.mirea.isp.domain.PriorityLevel;
import ru.mirea.isp.domain.ConnectionRequestStatus;

import java.math.BigDecimal;
import java.util.List;

/** Результат безопасного разбора строки умного фильтра. */
public record ConnectionRequestFilter(
        String freeText,
        String subscriber,
        ConnectionRequestStatus status,
        Boolean urgent,
        PriorityLevel priority,
        String tag,
        Boolean archived,
        List<HoursConstraint> hours) {

    public enum Operator { LT, LTE, EQ, GTE, GT }
    public record HoursConstraint(Operator operator, BigDecimal value) { }
}
