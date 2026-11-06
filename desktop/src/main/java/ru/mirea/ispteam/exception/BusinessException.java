package ru.mirea.ispteam.exception;

/*
 * Владелец: B (обработка ошибок). Скелет создан A.
 * Нарушение бизнес-правила (переходы статусов, ограничения на удаление и т.п.).
 * См. бизнес-правила 3, 4, 5 в PKS.md разд. 2.4.
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
