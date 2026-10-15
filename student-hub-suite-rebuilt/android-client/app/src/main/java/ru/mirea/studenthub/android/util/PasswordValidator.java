package ru.mirea.studenthub.android.util;

import java.util.Locale;

/** Клиентская копия базовых правил PasswordPolicy для мгновенной обратной связи. */
public final class PasswordValidator {
    private PasswordValidator() {
    }

    /** Возвращает null, если пароль проходит проверку, иначе русский текст ошибки. */
    public static String validate(String password, String email) {
        if (password == null || password.length() < 12 || password.length() > 128) {
            return "Пароль должен содержать от 12 до 128 символов";
        }

        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        boolean special = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));
        if (!upper || !lower || !digit || !special) {
            return "Пароль должен содержать верхний и нижний регистр, цифру и специальный символ";
        }

        String localPart = email == null ? "" : email.toLowerCase(Locale.ROOT).split("@", 2)[0];
        if (localPart.length() >= 4 && password.toLowerCase(Locale.ROOT).contains(localPart)) {
            return "Пароль не должен содержать имя учётной записи";
        }

        return null;
    }
}
