package ru.mirea.studenthub.domain;

/**
 * Роли прикладного доступа. Сервер проверяет их независимо от того, какие кнопки
 * скрыл конкретный клиент.
 */
public enum Role {
    VIEWER,
    TEACHER,
    ADMIN
}
