package ru.mirea.studenthub.fx.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;

/**
 * Диалог первичной и обычной смены пароля.
 *
 * <p>Первичная смена не запрашивает временный пароль повторно: аутентификация уже
 * состоялась. Обычная смена постоянного пароля по-прежнему требует текущий пароль.</p>
 */
public final class PasswordDialog {
    private PasswordDialog() { }

    /** Обязательная первичная установка постоянного пароля. */
    public static boolean showInitial(Window owner, ApiClient api) {
        return show(owner, api, true);
    }

    /** Добровольная смена существующего постоянного пароля. */
    public static boolean showRegular(Window owner, ApiClient api) {
        return show(owner, api, false);
    }

    private static boolean show(Window owner, ApiClient api, boolean initial) {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(initial ? "Установка постоянного пароля" : "Смена пароля");
        dialog.setHeaderText("Минимум 12 символов: верхний/нижний регистр, цифра и специальный символ");

        ButtonType save = new ButtonType(initial ? "Установить пароль" : "Изменить",
                ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        PasswordField current = new PasswordField();
        current.setPromptText("Текущий пароль");
        PasswordField next = new PasswordField();
        next.setPromptText("Новый пароль");
        PasswordField repeat = new PasswordField();
        repeat.setPromptText("Повторите новый пароль");

        Label error = new Label();
        error.getStyleClass().add("error-label");
        error.setWrapText(true);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int row = 0;
        if (!initial) {
            grid.addRow(row++, new Label("Текущий пароль:"), current);
        }
        grid.addRow(row++, new Label("Новый пароль:"), next);
        grid.addRow(row++, new Label("Повторите пароль:"), repeat);
        grid.add(error, 0, row, 2, 1);

        dialog.getDialogPane().setContent(grid);
        dialog.setResultConverter(button -> Boolean.FALSE);

        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            event.consume();
            error.setText("");

            if (!initial && current.getText().isBlank()) {
                error.setText("Введите текущий пароль");
                return;
            }
            if (next.getText().isBlank() || repeat.getText().isBlank()) {
                error.setText("Введите новый пароль дважды");
                return;
            }
            if (!next.getText().equals(repeat.getText())) {
                error.setText("Новые пароли не совпадают");
                return;
            }
            String policyError = validatePassword(next.getText());
            if (policyError != null) {
                error.setText(policyError);
                return;
            }

            saveButton.setDisable(true);
            String currentValue = current.getText();
            String nextValue = next.getText();

            FxTasks.run(() -> {
                if (initial) api.changeInitialPassword(nextValue);
                else api.changePassword(currentValue, nextValue);
                return null;
            }, ignored -> {
                current.clear();
                next.clear();
                repeat.clear();
                dialog.setResult(Boolean.TRUE);
                dialog.close();
            }, throwable -> {
                saveButton.setDisable(false);
                error.setText(FxTasks.friendly(throwable));
            });
        });

        return dialog.showAndWait().orElse(Boolean.FALSE);
    }

    private static String validatePassword(String value) {
        if (value.length() < 12) return "Пароль должен содержать не менее 12 символов";
        if (value.chars().noneMatch(Character::isUpperCase)) return "Добавьте хотя бы одну заглавную букву";
        if (value.chars().noneMatch(Character::isLowerCase)) return "Добавьте хотя бы одну строчную букву";
        if (value.chars().noneMatch(Character::isDigit)) return "Добавьте хотя бы одну цифру";
        if (value.chars().allMatch(Character::isLetterOrDigit)) return "Добавьте хотя бы один специальный символ";
        return null;
    }
}
