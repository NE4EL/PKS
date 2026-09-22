package ru.mirea.ispteam.model;

/*
 * Владелец: D (модель). Скелет создан A.
 * Эталон из PKS.md разд. 2.3 — состав значений менять только по согласованию команды.
 */
public enum RequestStatus {
    NEW,
    IN_PROGRESS,
    APPROVED,
    COMPLETED,
    REJECTED,
    CANCELLED
}
