package ru.mirea.studenthub.fx.ui;

import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

/** PasswordField-диалог; обычный TextInputDialog для пароля не используется. */
public final class PasswordPrompt {
    private PasswordPrompt() { }

    public static Optional<String> show(Window owner,String title){
        Dialog<String> dialog=new Dialog<>();dialog.initOwner(owner);dialog.setTitle(title);ButtonType ok=new ButtonType("Подтвердить",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(ok,ButtonType.CANCEL);
        PasswordField field=new PasswordField();Label hint=new Label("Минимум 12 символов, регистры, цифра и специальный символ");hint.getStyleClass().add("muted");Label error=new Label();error.getStyleClass().add("error-label");dialog.getDialogPane().setContent(new VBox(8,field,hint,error));
        Node button=dialog.getDialogPane().lookupButton(ok);button.addEventFilter(ActionEvent.ACTION,event->{if(field.getText().length()<12){error.setText("Пароль слишком короткий");event.consume();}});
        dialog.setResultConverter(result->result==ok?field.getText():null);return dialog.showAndWait();
    }
}
