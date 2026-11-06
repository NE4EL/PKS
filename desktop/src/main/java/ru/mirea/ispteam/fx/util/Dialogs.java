package ru.mirea.ispteam.fx.util;

import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.mirea.ispteam.exception.BusinessException;
import ru.mirea.ispteam.exception.DatabaseException;
import ru.mirea.ispteam.exception.EntityNotFoundException;
import ru.mirea.ispteam.exception.ExportException;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.fx.ViewLoader;

import java.util.List;
import java.util.Optional;

/**
 * Стандартные диалоговые окна приложения: ошибка, подтверждение, информация, выбор.
 *
 * Обработка исключений в одном месте: каждое исключение превращается в понятное сообщение
 * для пользователя. Stack trace пользователю не показывается — он пишется в журнал (slf4j).
 */
public final class Dialogs {

    private static final Logger log = LoggerFactory.getLogger(Dialogs.class);

    private Dialogs() {
    }

    public static void showError(Window owner, Throwable error) {
        String title;
        String message = error.getMessage();
        if (error instanceof ValidationException) {
            title = "Некорректные данные";
        } else if (error instanceof BusinessException) {
            title = "Операция невозможна";
        } else if (error instanceof EntityNotFoundException) {
            title = "Запись не найдена";
            message += ". Возможно, её уже удалили — нажмите «Обновить».";
        } else if (error instanceof DatabaseException) {
            title = "Ошибка базы данных";
            message += ". Проверьте, что PostgreSQL запущен, и повторите действие.";
            log.error("Ошибка БД", error);
        } else if (error instanceof ExportException) {
            title = "Ошибка экспорта";
            log.error("Ошибка экспорта", error);
        } else {
            title = "Непредвиденная ошибка";
            message = "Действие не выполнено. Подробности записаны в журнал приложения.";
            log.error("Непредвиденная ошибка", error);
        }
        showError(owner, title, message);
    }

    public static void showError(Window owner, String title, String message) {
        Alert alert = create(Alert.AlertType.ERROR, owner, title, message);
        alert.showAndWait();
    }

    public static void showInfo(Window owner, String title, String message) {
        create(Alert.AlertType.INFORMATION, owner, title, message).showAndWait();
    }

    /** Подтверждение опасного действия. По умолчанию выбрана кнопка «Отмена». */
    public static boolean confirm(Window owner, String title, String message, String actionText) {
        Alert alert = create(Alert.AlertType.CONFIRMATION, owner, title, message);
        ButtonType action = new ButtonType(actionText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(action, cancel);
        ((Button) alert.getDialogPane().lookupButton(action)).setDefaultButton(false);
        ((Button) alert.getDialogPane().lookupButton(cancel)).setDefaultButton(true);
        alert.getDialogPane().lookupButton(action).getStyleClass().add("danger");
        return alert.showAndWait().filter(action::equals).isPresent();
    }

    public static <T> Optional<T> choose(Window owner, String title, String header, List<Option<T>> options) {
        ChoiceDialog<Option<T>> dialog = new ChoiceDialog<>(options.get(0), options);
        dialog.initOwner(owner);
        dialog.setTitle(title);
        dialog.setHeaderText(header);
        dialog.setContentText("Выберите:");
        dialog.getDialogPane().getStylesheets().add(ViewLoader.stylesheet());
        return dialog.showAndWait().map(Option::value);
    }

    private static Alert create(Alert.AlertType type, Window owner, String title, String message) {
        Alert alert = new Alert(type);
        if (owner != null) alert.initOwner(owner);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.getDialogPane().getStylesheets().add(ViewLoader.stylesheet());
        alert.getDialogPane().setMinWidth(440);
        return alert;
    }
}
