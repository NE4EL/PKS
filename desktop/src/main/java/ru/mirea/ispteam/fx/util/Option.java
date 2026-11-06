package ru.mirea.ispteam.fx.util;

/**
 * Пункт выпадающего списка: подпись для пользователя + значение.
 * Нужен там, где кроме конкретных значений есть пункт «Все» (value = null):
 * ComboBox показывает toString(), то есть подпись.
 */
public record Option<T>(String label, T value) {

    @Override
    public String toString() {
        return label;
    }
}
