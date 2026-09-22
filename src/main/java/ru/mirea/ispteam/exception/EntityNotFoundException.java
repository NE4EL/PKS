package ru.mirea.ispteam.exception;

/*
 * Владелец: B (обработка ошибок). Скелет создан A.
 * Кидается, когда сущность с заданным id не найдена (см. бизнес-правило 2 в PKS.md).
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String message) {
        super(message);
    }
}
