package ru.mirea.studenthub.fx.ui;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToolBar;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;
import ru.mirea.studenthub.fx.util.Ui;

import java.io.File;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Основной экран студентов: умный фильтр, CRUD, архив, заметки, массовые операции и CSV.
 */
public final class StudentsPane extends BorderPane {
    private final ApiClient api;
    private final Window owner;
    private final TextField search = new TextField();
    private final TableView<Models.Student> table = new TableView<>();
    private final ProgressIndicator progress = new ProgressIndicator();
    private final Label pageLabel = new Label();
    private final Button previous = new Button("←");
    private final Button next = new Button("→");
    private final Button add = new Button("Добавить");
    private final Button edit = new Button("Изменить");
    private final Button archive = new Button("В архив");
    private final Button notes = new Button("Заметки");
    private int currentPage;
    private int totalPages;
    private List<Models.Group> groups = List.of();

    public StudentsPane(ApiClient api, Window owner) {
        this.api = api;
        this.owner = owner;
        setPadding(new Insets(16));
        setTop(buildToolbar());
        configureTable();
        setCenter(table);
        setBottom(buildPager());
        loadGroupsAndStudents();
    }

    private ToolBar buildToolbar() {
        search.setPromptText("Иванов группа:ЭФБО-07-24 балл<60 долг:да риск:высокий");
        search.setPrefWidth(480);
        search.setOnKeyPressed(event -> { if (event.getCode() == KeyCode.ENTER) load(0); });
        Button find = new Button("Найти");
        find.getStyleClass().add("primary-button");
        find.setOnAction(event -> load(0));
        add.setOnAction(event -> createStudent());
        edit.setOnAction(event -> editStudent(false));
        notes.setOnAction(event -> openNotes());
        archive.setOnAction(event -> toggleArchive());
        Button bulk = new Button("Массово");
        bulk.setOnAction(event -> bulkAction());
        Button importCsv = new Button("Импорт CSV");
        importCsv.setOnAction(event -> importCsv());
        Button exportCsv = new Button("Экспорт CSV");
        exportCsv.setOnAction(event -> exportCsv());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        progress.setMaxSize(22, 22); progress.setVisible(false);

        ToolBar toolbar = new ToolBar(search, find, progress, spacer);
        if (api.currentUser().canEdit()) toolbar.getItems().addAll(add, edit, notes, archive, bulk, importCsv, exportCsv);
        return toolbar;
    }

    private void configureTable() {
        table.getSelectionModel().setSelectionMode(javafx.scene.control.SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(stringColumn("ФИО", Models.Student::fullName, 220));
        table.getColumns().add(stringColumn("Группа", s -> value(s.groupCode()), 110));
        table.getColumns().add(stringColumn("Статус", s -> statusLabel(s.status()), 150));
        table.getColumns().add(stringColumn("Балл", s -> s.averageGrade() == null ? "—" : s.averageGrade().toPlainString(), 80));
        TableColumn<Models.Student, Boolean> debt = new TableColumn<>("Долг");
        debt.setCellValueFactory(data -> new ReadOnlyBooleanWrapper(data.getValue().hasDebt()));
        debt.setPrefWidth(70);
        table.getColumns().add(debt);
        table.getColumns().add(stringColumn("Риск", s -> riskLabel(s.riskLevel()), 100));
        table.getColumns().add(stringColumn("Теги", s -> s.tags() == null ? "" : String.join(", ", s.tags()), 170));
        table.getColumns().add(stringColumn("Архив", s -> s.archived() ? "Да" : "Нет", 70));
        table.setPlaceholder(Ui.emptyLabel("Студенты не найдены"));
        table.getSelectionModel().selectedItemProperty().addListener((obs, old, selected) -> updateButtons(selected));
        table.setOnMouseClicked(event -> { if (event.getClickCount() == 2 && table.getSelectionModel().getSelectedItem() != null) editStudent(true); });
        updateButtons(null);
    }

    private HBox buildPager() {
        previous.setOnAction(event -> load(currentPage - 1));
        next.setOnAction(event -> load(currentPage + 1));
        HBox box = new HBox(10, previous, pageLabel, next);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12));
        return box;
    }

    private void loadGroupsAndStudents() {
        setBusy(true);
        FxTasks.run(() -> api.groups(false), result -> {
            groups = result;
            load(0);
        }, error -> { setBusy(false); Ui.error(owner, error); });
    }

    private void load(int page) {
        if (page < 0) return;
        setBusy(true);
        FxTasks.run(() -> api.students(search.getText().trim(), page, 25, "name,asc"), result -> {
            setBusy(false);
            currentPage = result.page();
            totalPages = result.totalPages();
            table.setItems(FXCollections.observableArrayList(result.content()));
            pageLabel.setText("Страница " + (currentPage + 1) + " из " + Math.max(1, totalPages) + " · " + result.totalElements());
            previous.setDisable(result.first());
            next.setDisable(result.last());
            updateButtons(table.getSelectionModel().getSelectedItem());
        }, error -> { setBusy(false); Ui.error(owner, error); });
    }

    private void createStudent() {
        StudentDialog.show(owner, groups, null, false).ifPresent(request -> {
            setBusy(true);
            FxTasks.run(() -> api.createStudent(request), ignored -> { setBusy(false); load(currentPage); },
                    error -> { setBusy(false); Ui.error(owner, error); });
        });
    }

    private void editStudent(boolean readOnly) {
        Models.Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        StudentDialog.show(owner, groups, selected, readOnly || !api.currentUser().canEdit()).ifPresent(request -> {
            setBusy(true);
            FxTasks.run(() -> api.updateStudent(selected.id(), request), ignored -> { setBusy(false); load(currentPage); },
                    error -> { setBusy(false); Ui.error(owner, error); });
        });
    }

    private void toggleArchive() {
        Models.Student selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        String action = selected.archived() ? "восстановить" : "переместить в архив";
        if (!Ui.confirm(owner, "Действительно " + action + " карточку «" + selected.fullName() + "»?")) return;
        setBusy(true);
        FxTasks.run(() -> selected.archived() ? api.restoreStudent(selected.id()) : api.archiveStudent(selected),
                ignored -> { setBusy(false); load(currentPage); },
                error -> { setBusy(false); Ui.error(owner, error); });
    }

    private void openNotes() {
        Models.Student selected = table.getSelectionModel().getSelectedItem();
        if (selected != null) NotesDialog.show(owner, api, selected);
    }

    private void bulkAction() {
        Set<Models.Student> selected = new LinkedHashSet<>(table.getSelectionModel().getSelectedItems());
        if (selected.isEmpty()) { Ui.info(owner, "Выберите одну или несколько строк"); return; }
        ChoiceDialog<Models.BulkAction> dialog = new ChoiceDialog<>(Models.BulkAction.ARCHIVE, Models.BulkAction.values());
        dialog.initOwner(owner); dialog.setTitle("Массовая операция"); dialog.setHeaderText("Выбрано: " + selected.size());
        dialog.showAndWait().ifPresent(action -> {
            Models.StudentStatus status = null;
            String tag = null;
            if (action == Models.BulkAction.SET_STATUS) {
                ChoiceDialog<Models.StudentStatus> statusDialog = new ChoiceDialog<>(Models.StudentStatus.ENROLLED, Models.StudentStatus.values());
                statusDialog.initOwner(owner); statusDialog.setHeaderText("Новый статус");
                status = statusDialog.showAndWait().orElse(null);
                if (status == null) return;
            }
            if (action == Models.BulkAction.ADD_TAG || action == Models.BulkAction.REMOVE_TAG) {
                tag = Ui.prompt(owner, "Тег", "Введите тег").orElse(null);
                if (tag == null) return;
            }
            Models.StudentStatus finalStatus = status;
            String finalTag = tag;
            Set<java.util.UUID> ids = selected.stream().map(Models.Student::id).collect(java.util.stream.Collectors.toSet());
            setBusy(true);
            FxTasks.run(() -> api.bulk(ids, action, finalStatus, finalTag), result -> {
                setBusy(false);
                Ui.info(owner, "Успешно: " + result.succeeded() + "; ошибок: " + result.failures().size());
                load(currentPage);
            }, error -> { setBusy(false); Ui.error(owner, error); });
        });
    }

    private void importCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Выберите CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showOpenDialog(owner);
        if (file == null) return;
        ChoiceDialog<Models.ImportMode> modeDialog = new ChoiceDialog<>(Models.ImportMode.CREATE_ONLY, Models.ImportMode.values());
        modeDialog.initOwner(owner); modeDialog.setTitle("Режим импорта"); modeDialog.setHeaderText("Сначала будет выполнена проверка без записи");
        modeDialog.showAndWait().ifPresent(mode -> {
            setBusy(true);
            FxTasks.run(() -> api.previewImport(file.toPath(), mode), preview -> {
                setBusy(false);
                String summary = "Строк: " + preview.totalRows() + "\nВалидных: " + preview.validRows() +
                        "\nОшибок: " + preview.invalidRows() + formatIssues(preview.issues());
                if (!Ui.confirm(owner, summary + "\n\nПрименить валидные строки?")) return;
                setBusy(true);
                FxTasks.run(() -> api.applyImport(file.toPath(), mode), result -> {
                    setBusy(false);
                    Ui.info(owner, "Создано: " + result.created() + "; обновлено: " + result.updated() +
                            "; пропущено: " + result.skipped() + formatIssues(result.issues()));
                    load(0);
                }, error -> { setBusy(false); Ui.error(owner, error); });
            }, error -> { setBusy(false); Ui.error(owner, error); });
        });
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить CSV");
        chooser.setInitialFileName("students.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
        File file = chooser.showSaveDialog(owner);
        if (file == null) return;
        setBusy(true);
        FxTasks.run(() -> { api.exportStudents(search.getText().trim(), file.toPath()); return file.toPath(); }, path -> {
            setBusy(false); Ui.info(owner, "Файл сохранён: " + path);
        }, error -> { setBusy(false); Ui.error(owner, error); });
    }

    private void updateButtons(Models.Student selected) {
        boolean none = selected == null;
        edit.setDisable(none);
        notes.setDisable(none);
        archive.setDisable(none);
        archive.setText(selected != null && selected.archived() ? "Восстановить" : "В архив");
    }

    private void setBusy(boolean busy) {
        progress.setVisible(busy);
        search.setDisable(busy);
        table.setDisable(busy);
        add.setDisable(busy);
    }

    private static TableColumn<Models.Student, String> stringColumn(String title,
                                                                    java.util.function.Function<Models.Student, String> value,
                                                                    double width) {
        TableColumn<Models.Student, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new ReadOnlyStringWrapper(value.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    private static String formatIssues(List<Models.RowIssue> issues) {
        if (issues == null || issues.isEmpty()) return "";
        String lines = issues.stream().limit(10).map(i -> "\nСтрока " + i.row() + ", " + csvFieldLabel(i.field()) + ": " + lowerFirst(i.message()))
                .reduce("", String::concat);
        return lines + (issues.size() > 10 ? "\n… и ещё " + (issues.size() - 10) : "");
    }

    private static String csvFieldLabel(String field) {
        if (field == null) return "поле";
        return switch (field) {
            case "externalId" -> "внешний ID";
            case "firstName" -> "имя";
            case "lastName" -> "фамилия";
            case "middleName" -> "отчество";
            case "email" -> "email";
            case "phone" -> "телефон";
            case "birthDate" -> "дата рождения";
            case "group", "groupCode" -> "группа";
            case "status" -> "статус";
            case "averageGrade" -> "средний балл";
            case "hasDebt" -> "задолженность";
            case "tags" -> "теги";
            default -> field;
        };
    }

    private static String lowerFirst(String text) {
        if (text == null || text.isBlank()) return "";
        return Character.toLowerCase(text.charAt(0)) + text.substring(1);
    }

    private static String value(String value) { return value == null || value.isBlank() ? "—" : value; }
    private static String statusLabel(Models.StudentStatus value) {
        return value == null ? "—" : value.toString();
    }

    private static String riskLabel(Models.RiskLevel value) {
        return value == null ? "—" : value.toString();
    }
}
