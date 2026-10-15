package ru.mirea.studenthub.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Window;

import java.util.Optional;

/** Общие безопасные диалоги JavaFX без дублирования. */
public final class Ui {
    private Ui() { }

    public static void error(Window owner, Throwable error) {
        Alert alert = new Alert(Alert.AlertType.ERROR, FxTasks.friendly(error), ButtonType.OK);
        alert.initOwner(owner);
        alert.setHeaderText("Операция не выполнена");
        alert.showAndWait();
    }

    public static void info(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
        alert.initOwner(owner);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    public static boolean confirm(Window owner, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.YES, ButtonType.NO);
        alert.initOwner(owner);
        alert.setHeaderText("Подтверждение");
        return alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }

    public static Optional<String> prompt(Window owner, String title, String message) {
        TextInputDialog dialog = new TextInputDialog();
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(message);
        return dialog.showAndWait().map(String::trim).filter(value -> !value.isEmpty());
    }

    public static Label emptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }
}
