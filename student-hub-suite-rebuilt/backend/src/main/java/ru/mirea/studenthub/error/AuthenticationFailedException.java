package ru.mirea.studenthub.error;

/**
 * Общая ошибка входа без указания, существует ли введённый email.
 * Это уменьшает возможность перебора зарегистрированных учётных записей.
 */
public class AuthenticationFailedException extends RuntimeException {
    public AuthenticationFailedException() { super("Неверные учётные данные или доступ временно заблокирован"); }
}
