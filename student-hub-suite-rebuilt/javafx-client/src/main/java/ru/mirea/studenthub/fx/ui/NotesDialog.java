package ru.mirea.studenthub.fx.ui;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;
import ru.mirea.studenthub.fx.util.Ui;

/** Полный CRUD заметок выбранного студента. */
public final class NotesDialog {
    private NotesDialog() { }

    public static void show(Window owner, ApiClient api, Models.Student student) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(owner); dialog.setTitle("Заметки — " + student.fullName());
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        ListView<Models.Note> list = new ListView<>();
        list.setPrefSize(650, 420);
        list.setCellFactory(view -> new ListCell<>() {
            @Override protected void updateItem(Models.Note note, boolean empty) {
                super.updateItem(note, empty);
                if (empty || note == null) { setText(null); setGraphic(null); }
                else {
                    Label meta = new Label((note.pinned() ? "📌 " : "") + note.authorName() + " · " + note.createdAt());
                    meta.getStyleClass().add("muted");
                    Label body = new Label(note.body()); body.setWrapText(true);
                    VBox box = new VBox(4, meta, body); box.setPadding(new Insets(6));
                    setGraphic(box); setText(null);
                }
            }
        });
        Button add = new Button("Добавить"); add.getStyleClass().add("primary-button");
        Button edit = new Button("Изменить"); Button delete = new Button("Удалить"); delete.getStyleClass().add("danger-button");
        HBox buttons = new HBox(8, add, edit, delete);
        BorderPane content = new BorderPane(list); content.setBottom(buttons); BorderPane.setMargin(buttons, new Insets(10,0,0,0)); content.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(content);

        Runnable reload = () -> FxTasks.run(() -> api.notes(student.id()), notes -> list.setItems(FXCollections.observableArrayList(notes)), error -> Ui.error(owner,error));
        add.setOnAction(event -> noteEditor(owner, null).ifPresent(input -> FxTasks.run(() -> api.createNote(student.id(), input.body, input.pinned), ignored -> reload.run(), error -> Ui.error(owner,error))));
        edit.setOnAction(event -> {
            Models.Note selected = list.getSelectionModel().getSelectedItem(); if (selected == null) return;
            noteEditor(owner, selected).ifPresent(input -> FxTasks.run(() -> api.updateNote(student.id(), selected, input.body, input.pinned), ignored -> reload.run(), error -> Ui.error(owner,error)));
        });
        delete.setOnAction(event -> {
            Models.Note selected = list.getSelectionModel().getSelectedItem(); if (selected == null || !Ui.confirm(owner,"Удалить заметку?")) return;
            FxTasks.run(() -> { api.deleteNote(student.id(), selected); return null; }, ignored -> reload.run(), error -> Ui.error(owner,error));
        });
        reload.run();
        dialog.showAndWait();
    }

    private static java.util.Optional<NoteInput> noteEditor(Window owner, Models.Note note) {
        Dialog<NoteInput> dialog = new Dialog<>(); dialog.initOwner(owner); dialog.setTitle(note == null ? "Новая заметка" : "Изменение заметки");
        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE); dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextArea body = new TextArea(note == null ? "" : note.body()); body.setWrapText(true); body.setPrefRowCount(8);
        CheckBox pinned = new CheckBox("Закрепить"); pinned.setSelected(note != null && note.pinned());
        dialog.getDialogPane().setContent(new VBox(8, body, pinned));
        dialog.getDialogPane().lookupButton(save).disableProperty().bind(body.textProperty().isEmpty());
        dialog.setResultConverter(button -> button == save ? new NoteInput(body.getText().trim(), pinned.isSelected()) : null);
        return dialog.showAndWait();
    }

    private record NoteInput(String body, boolean pinned) { }
}
