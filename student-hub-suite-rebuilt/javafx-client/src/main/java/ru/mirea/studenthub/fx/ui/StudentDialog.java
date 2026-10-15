package ru.mirea.studenthub.fx.ui;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.util.PhoneFormat;
import ru.mirea.studenthub.fx.util.PhoneMask;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Формирует типобезопасный запрос создания/изменения студента. */
public final class StudentDialog {
    private StudentDialog() { }

    public static Optional<Models.StudentRequest> show(Window owner, List<Models.Group> groups,
                                                        Models.Student student, boolean readOnly) {
        Dialog<Models.StudentRequest> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(student == null ? "Новый студент" : readOnly ? "Карточка студента" : "Изменение студента");
        dialog.setHeaderText(readOnly ? student.fullName() : "Сервер повторно проверит все поля и рассчитает риск");
        ButtonType save = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        if (!readOnly) dialog.getDialogPane().getButtonTypes().add(save);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        TextField lastName = field(student == null ? null : student.lastName());
        TextField firstName = field(student == null ? null : student.firstName());
        TextField middleName = field(student == null ? null : student.middleName());
        TextField externalId = field(student == null ? null : student.externalId());
        TextField email = field(student == null ? null : student.email());
        TextField phone = field(student == null ? null : PhoneFormat.normalize(student.phone()));
        phone.setPromptText("+7-999-999-99-99");
        if (!readOnly) PhoneMask.install(phone);
        DatePicker birthDate = new DatePicker(student == null ? null : student.birthDate());
        ComboBox<Models.Group> group = new ComboBox<>();
        group.getItems().addAll(groups);
        if (student != null && student.groupId() != null) groups.stream().filter(g -> g.id().equals(student.groupId())).findFirst().ifPresent(group::setValue);
        group.setPromptText("Без группы");
        ComboBox<Models.StudentStatus> status = new ComboBox<>();
        status.getItems().addAll(Models.StudentStatus.values());
        status.setValue(student == null ? Models.StudentStatus.ENROLLED : student.status());
        TextField grade = field(student == null || student.averageGrade() == null ? null : student.averageGrade().toPlainString());
        CheckBox debt = new CheckBox("Есть академическая задолженность");
        debt.setSelected(student != null && student.hasDebt());
        TextField tags = field(student == null || student.tags() == null ? null : String.join(", ", student.tags()));
        Label error = new Label(); error.getStyleClass().add("error-label"); error.setWrapText(true);

        GridPane grid = new GridPane(); grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(10));
        add(grid, 0, "Фамилия", lastName); add(grid, 1, "Имя", firstName); add(grid, 2, "Отчество", middleName);
        add(grid, 3, "External ID", externalId); add(grid, 4, "Email", email); add(grid, 5, "Телефон", phone);
        add(grid, 6, "Дата рождения", birthDate); add(grid, 7, "Группа", group); add(grid, 8, "Статус", status);
        grade.setPromptText("0–100");
        add(grid, 9, "Средний балл (0–100)", grade); grid.add(debt, 1, 10); add(grid, 11, "Теги через запятую", tags);
        grid.add(error, 0, 12, 2, 1);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(620);

        List<Node> controls = List.of(lastName, firstName, middleName, externalId, email, phone,
                birthDate, group, status, grade, debt, tags);
        controls.forEach(node -> node.setDisable(readOnly));

        if (!readOnly) {
            Node saveButton = dialog.getDialogPane().lookupButton(save);
            saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                String validation = validate(lastName.getText(), firstName.getText(), email.getText(), phone.getText(), grade.getText());
                if (validation != null) { error.setText(validation); event.consume(); }
            });
            dialog.setResultConverter(button -> button == save ? new Models.StudentRequest(
                    empty(externalId.getText()), firstName.getText().trim(), lastName.getText().trim(),
                    empty(middleName.getText()), empty(email.getText()), empty(phone.getText()),
                    birthDate.getValue(), group.getValue() == null ? null : group.getValue().id(),
                    status.getValue(), grade.getText().isBlank() ? null : new BigDecimal(grade.getText().replace(',', '.')),
                    debt.isSelected(), parseTags(tags.getText()), student == null ? null : student.version()) : null);
        }
        return dialog.showAndWait();
    }

    private static void add(GridPane grid, int row, String label, Node field) {
        grid.add(new Label(label + ":"), 0, row); grid.add(field, 1, row);
        GridPane.setHgrow(field, javafx.scene.layout.Priority.ALWAYS);
    }

    private static TextField field(String value) { TextField field = new TextField(value == null ? "" : value); field.setMaxWidth(Double.MAX_VALUE); return field; }

    private static String validate(String lastName, String firstName, String email, String phone, String grade) {
        if (lastName.isBlank() || firstName.isBlank()) return "Фамилия и имя обязательны";
        if (!email.isBlank() && !email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) return "Некорректный email";
        if (!phone.isBlank() && !PhoneFormat.isComplete(phone)) return "Телефон должен иметь формат +7-999-999-99-99";
        if (!grade.isBlank()) {
            try {
                BigDecimal value = new BigDecimal(grade.replace(',', '.'));
                if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) return "Средний балл должен быть от 0 до 100";
            } catch (NumberFormatException ex) { return "Средний балл должен быть числом"; }
        }
        return null;
    }

    private static Set<String> parseTags(String source) {
        LinkedHashSet<String> values = new LinkedHashSet<>();
        Arrays.stream(source.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(values::add);
        return values;
    }

    private static String empty(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
}
