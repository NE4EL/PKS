package ru.mirea.studenthub.domain;

/** Рассчитанный уровень академического риска. */
public enum RiskLevel {
    /** Риск не рассчитывается для завершённого/прекращённого обучения. */
    NOT_APPLICABLE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
