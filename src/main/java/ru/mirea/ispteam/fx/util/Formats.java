package ru.mirea.ispteam.fx.util;

import javafx.util.StringConverter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.function.Function;

/**
 * Единые форматы отображения дат, чисел и значений в элементах JavaFX.
 */
public final class Formats {

    public static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private Formats() {
    }

    public static String date(LocalDate date) {
        return date == null ? "—" : date.format(DATE);
    }

    public static String dateTime(LocalDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.format(DATE_TIME);
    }

    public static String text(String value) {
        return value == null || value.isBlank() ? "—" : value;
    }

    public static String number(double value) {
        return String.format(Locale.forLanguageTag("ru"), "%.1f", value);
    }

    /** Длительность в часах -> «2 д 5 ч» или «3,5 ч». */
    public static String hours(double hours) {
        if (hours >= 24) {
            long days = (long) (hours / 24);
            long rest = Math.round(hours - days * 24);
            return rest == 0 ? days + " д" : days + " д " + rest + " ч";
        }
        return number(hours) + " ч";
    }

    /** Конвертер для ComboBox: показывает подпись объекта, обратное преобразование не нужно. */
    public static <T> StringConverter<T> converter(Function<T, String> label) {
        return new StringConverter<>() {
            @Override
            public String toString(T value) {
                return value == null ? "" : label.apply(value);
            }

            @Override
            public T fromString(String text) {
                return null;
            }
        };
    }

    /**
     * Конвертер для DatePicker в формате дд.ММ.гггг. Некорректно введённая вручную дата
     * не роняет приложение, а превращается в «пусто».
     */
    public static StringConverter<LocalDate> dateConverter() {
        return new StringConverter<>() {
            @Override
            public String toString(LocalDate date) {
                return date == null ? "" : date.format(DATE);
            }

            @Override
            public LocalDate fromString(String text) {
                if (text == null || text.isBlank()) return null;
                try {
                    return LocalDate.parse(text.strip(), DATE);
                } catch (DateTimeParseException e) {
                    return null;
                }
            }
        };
    }
}
