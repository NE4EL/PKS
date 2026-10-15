package ru.mirea.studenthub.fx.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;

import java.util.Optional;

/** Диалог учебной группы. */
public final class GroupDialog {
    private GroupDialog() { }

    public static Optional<Models.GroupRequest> show(Window owner, Models.Group group) {
        Dialog<Models.GroupRequest> dialog = new Dialog<>(); dialog.initOwner(owner); dialog.setTitle(group == null ? "Новая группа" : "Изменение группы");
        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL);
        TextField code = new TextField(group == null ? "" : group.code());
        TextField name = new TextField(group == null || group.name() == null ? "" : group.name());
        TextField year = new TextField(group == null || group.admissionYear() == null ? "" : group.admissionYear().toString());
        CheckBox active = new CheckBox("Активна"); active.setSelected(group == null || group.active());
        Label error = new Label(); error.getStyleClass().add("error-label");
        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        grid.addRow(0,new Label("Код:"),code); grid.addRow(1,new Label("Название:"),name); grid.addRow(2,new Label("Год поступления:"),year); grid.add(active,1,3); grid.add(error,0,4,2,1);
        dialog.getDialogPane().setContent(grid);
        Node button = dialog.getDialogPane().lookupButton(save);
        button.addEventFilter(ActionEvent.ACTION,event->{
            if(code.getText().isBlank()){error.setText("Код обязателен");event.consume();return;}
            if(!year.getText().isBlank())try{int y=Integer.parseInt(year.getText());if(y<2000||y>2100)throw new NumberFormatException();}catch(NumberFormatException ex){error.setText("Год должен быть от 2000 до 2100");event.consume();}
        });
        dialog.setResultConverter(result->result==save?new Models.GroupRequest(code.getText().trim(),empty(name.getText()),year.getText().isBlank()?null:Integer.parseInt(year.getText()),active.isSelected(),group==null?null:group.version()):null);
        return dialog.showAndWait();
    }
    private static String empty(String value){return value==null||value.isBlank()?null:value.trim();}
}
