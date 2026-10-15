package ru.mirea.studenthub.filter;

import ru.mirea.studenthub.domain.RiskLevel;
import ru.mirea.studenthub.domain.StudentStatus;

import java.math.BigDecimal;
import java.util.List;

/** Результат безопасного разбора строки умного фильтра. */
public record StudentFilter(
        String freeText,
        String group,
        StudentStatus status,
        Boolean debt,
        RiskLevel risk,
        String tag,
        Boolean archived,
        List<GradeConstraint> grades) {

    public enum Operator { LT, LTE, EQ, GTE, GT }
    public record GradeConstraint(Operator operator, BigDecimal value) { }
}
