package ru.mirea.ispteam.exception;

/*
 * Владелец: B (обработка ошибок). Скелет создан A.
 * Ошибка валидации входных данных (формат телефона/email, пустые обязательные поля,
 * слишком короткое описание). См. бизнес-правила 1, 6 в PKS.md разд. 2.4.
 *
 * КР2: необязательное поле field — ключ поля формы (например, "phone"), которое заполнено
 * неверно. JavaFX-форма по нему подсвечивает нужный элемент ввода.
 */
public class ValidationException extends RuntimeException {

    private final String field;

    public ValidationException(String message) {
        this(null, message);
    }

    public ValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
