package ru.mirea.ispteam.exception;

/*
 * Владелец: B (обработка ошибок). Скелет создан A.
 * Ошибка валидации входных данных (формат телефона/email, пустые обязательные поля,
 * слишком короткое описание). См. бизнес-правила 1, 6 в PKS.md разд. 2.4.
 */
public class ValidationException extends RuntimeException {

    public ValidationException(String message) {
        super(message);
    }
}
