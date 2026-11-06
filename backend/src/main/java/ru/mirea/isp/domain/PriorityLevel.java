package ru.mirea.isp.domain;

/** Рассчитанный уровень приоритета заявки. */
public enum PriorityLevel {
    /** Приоритет не рассчитывается для завершённых заявок. */
    NOT_APPLICABLE,
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
}
