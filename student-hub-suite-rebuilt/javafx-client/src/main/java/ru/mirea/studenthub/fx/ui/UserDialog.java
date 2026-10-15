package ru.mirea.studenthub.fx.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/** Диалог создания и изменения пользователя. */
public final class UserDialog {
    private UserDialog() { }

    public static Optional<UserInput> show(Window owner, Models.User user){
        Dialog<UserInput> dialog=new Dialog<>();dialog.initOwner(owner);dialog.setTitle(user==null?"Новый пользователь":"Изменение пользователя");
        ButtonType save=new ButtonType("Сохранить",ButtonBar.ButtonData.OK_DONE);dialog.getDialogPane().getButtonTypes().addAll(save,ButtonType.CANCEL);
        TextField email=new TextField(user==null?"":user.email());TextField name=new TextField(user==null?"":user.displayName());PasswordField password=new PasswordField();
        CheckBox viewer=new CheckBox(Models.Role.VIEWER.toString());CheckBox teacher=new CheckBox(Models.Role.TEACHER.toString());CheckBox admin=new CheckBox(Models.Role.ADMIN.toString());
        viewer.setSelected(user==null||user.roles().contains(Models.Role.VIEWER));teacher.setSelected(user!=null&&user.roles().contains(Models.Role.TEACHER));admin.setSelected(user!=null&&user.roles().contains(Models.Role.ADMIN));
        CheckBox enabled=new CheckBox("Активен");enabled.setSelected(user==null||user.enabled());Label error=new Label();error.getStyleClass().add("error-label");error.setWrapText(true);
        GridPane grid=new GridPane();grid.setHgap(10);grid.setVgap(10);grid.setPadding(new Insets(10));grid.addRow(0,new Label("Email:"),email);grid.addRow(1,new Label("Имя:"),name);
        int row=2;if(user==null){grid.addRow(row++,new Label("Временный пароль:"),password);}grid.add(new VBox(5,viewer,teacher,admin,enabled),1,row++);grid.add(error,0,row,2,1);dialog.getDialogPane().setContent(grid);
        Node button=dialog.getDialogPane().lookupButton(save);button.addEventFilter(ActionEvent.ACTION,event->{
            if(email.getText().isBlank()||name.getText().isBlank()){error.setText("Email и имя обязательны");event.consume();return;}
            if(!email.getText().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")){error.setText("Некорректный email");event.consume();return;}
            if(user==null&&password.getText().length()<12){error.setText("Временный пароль должен содержать не менее 12 символов");event.consume();return;}
            if(!viewer.isSelected()&&!teacher.isSelected()&&!admin.isSelected()){error.setText("Выберите хотя бы одну роль");event.consume();}
        });
        dialog.setResultConverter(result->{if(result!=save)return null;EnumSet<Models.Role> roles=EnumSet.noneOf(Models.Role.class);if(viewer.isSelected())roles.add(Models.Role.VIEWER);if(teacher.isSelected())roles.add(Models.Role.TEACHER);if(admin.isSelected())roles.add(Models.Role.ADMIN);return new UserInput(email.getText().trim(),name.getText().trim(),password.getText(),enabled.isSelected(),Set.copyOf(roles));});
        return dialog.showAndWait();
    }

    public record UserInput(String email,String name,String password,boolean enabled,Set<Models.Role> roles){}
}
