package ru.mirea.studenthub.fx.util;

import javafx.scene.control.TextField;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Применяет к TextField маску {@code +7-999-999-99-99}.
 *
 * <p>Из введённого значения используются только цифры; +7 и разделители добавляются
 * автоматически. Итоговый номер дополнительно проверяется на сервере.</p>
 */
public final class PhoneMask {
    private PhoneMask() { }

    public static void install(TextField field) {
        AtomicBoolean updating = new AtomicBoolean();

        field.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (focused && field.getText().isBlank()) {
                field.setText("+7-");
                field.positionCaret(field.getText().length());
            } else if (!focused && "+7-".equals(field.getText())) {
                field.clear();
            }
        });

        field.textProperty().addListener((observable, oldValue, newValue) -> {
            if (updating.get()) return;

            String normalized = PhoneFormat.normalize(newValue);
            if (!normalized.equals(newValue)) {
                updating.set(true);
                try {
                    field.setText(normalized);
                    field.positionCaret(normalized.length());
                } finally {
                    updating.set(false);
                }
            }
        });
    }
}
