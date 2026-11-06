package ru.mirea.ispteam.fx.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import ru.mirea.ispteam.exception.BusinessException;
import ru.mirea.ispteam.exception.EntityNotFoundException;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.fx.util.Dialogs;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Общая логика форм добавления/редактирования (без дублирования в двух контроллерах).
 *
 * Все проверки данных выполняет сервисный слой. Если сервис бросил ValidationException с ключом
 * поля (например, "phone"), форма подсвечивает это поле и показывает текст ошибки под ним.
 *
 * @param <T> тип сохраняемого объекта
 */
public abstract class FormController<T> {

    private static final String ERROR_STYLE = "field-error";

    private final Map<String, Control> fields = new HashMap<>();
    private T result;

    @FXML
    protected Label errorLabel;

    /** Связывает ключ поля из ValidationException с элементом ввода на форме. */
    protected void registerField(String key, Control control) {
        fields.put(key, control);
    }

    protected void clearErrors() {
        fields.values().forEach(control -> control.getStyleClass().remove(ERROR_STYLE));
        errorLabel.setText("");
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }

    protected void showError(RuntimeException error) {
        if (error instanceof ValidationException ve) {
            Control control = ve.getField() == null ? null : fields.get(ve.getField());
            if (control != null) {
                control.getStyleClass().add(ERROR_STYLE);
                control.requestFocus();
            }
            showMessage(ve.getMessage());
        } else if (error instanceof BusinessException || error instanceof EntityNotFoundException) {
            showMessage(error.getMessage());
        } else {
            // Ошибки БД и непредвиденные — отдельным окном (с записью в журнал).
            Dialogs.showError(stage(), error);
        }
    }

    protected void showMessage(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    /** Успешное сохранение: запоминаем результат и закрываем окно. */
    protected void finish(T saved) {
        result = saved;
        stage().close();
    }

    @FXML
    protected void onCancel() {
        stage().close();
    }

    public Optional<T> result() {
        return Optional.ofNullable(result);
    }

    protected Stage stage() {
        return (Stage) errorLabel.getScene().getWindow();
    }
}
