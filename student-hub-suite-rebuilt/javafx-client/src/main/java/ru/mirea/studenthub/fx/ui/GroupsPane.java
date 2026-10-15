package ru.mirea.studenthub.fx.ui;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;
import ru.mirea.studenthub.fx.util.Ui;

/** Справочник учебных групп с административным редактированием. */
public final class GroupsPane extends BorderPane {
    private final ApiClient api;
    private final Window owner;
    private final TableView<Models.Group> table = new TableView<>();
    private final Button edit = new Button("Изменить");
    private final Button toggle = new Button("Отключить");

    public GroupsPane(ApiClient api, Window owner) {
        this.api = api;
        this.owner = owner;
        setPadding(new Insets(16));
        setTop(buildToolbar());
        configureTable();
        setCenter(table);
        load();
    }

    private ToolBar buildToolbar() {
        Button refresh = new Button("Обновить"); refresh.setOnAction(event -> load());
        ToolBar toolbar = new ToolBar(refresh);
        if (api.currentUser().isAdmin()) {
            Button add = new Button("Добавить"); add.getStyleClass().add("primary-button"); add.setOnAction(event -> create());
            edit.setOnAction(event -> edit()); toggle.setOnAction(event -> toggle());
            toolbar.getItems().addAll(add, edit, toggle);
        }
        return toolbar;
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<Models.Group,String> code = new TableColumn<>("Код"); code.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().code()));
        TableColumn<Models.Group,String> name = new TableColumn<>("Название"); name.setCellValueFactory(d -> new ReadOnlyStringWrapper(d.getValue().name()));
        TableColumn<Models.Group,Integer> year = new TableColumn<>("Год"); year.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue().admissionYear()));
        TableColumn<Models.Group,Boolean> active = new TableColumn<>("Активна"); active.setCellValueFactory(d -> new ReadOnlyBooleanWrapper(d.getValue().active()));
        table.getColumns().addAll(code,name,year,active);
        table.setPlaceholder(Ui.emptyLabel("Группы не созданы"));
        table.getSelectionModel().selectedItemProperty().addListener((obs,old,value) -> {
            edit.setDisable(value == null); toggle.setDisable(value == null); toggle.setText(value != null && value.active() ? "Отключить" : "Включить");
        });
        edit.setDisable(true); toggle.setDisable(true);
    }

    private void load() {
        table.setDisable(true);
        FxTasks.run(() -> api.groups(false), values -> { table.setItems(FXCollections.observableArrayList(values)); table.setDisable(false); },
                error -> { table.setDisable(false); Ui.error(owner,error); });
    }

    private void create() {
        GroupDialog.show(owner,null).ifPresent(request -> FxTasks.run(() -> api.createGroup(request), ignored -> load(), error -> Ui.error(owner,error)));
    }

    private void edit() {
        Models.Group group = table.getSelectionModel().getSelectedItem();
        if (group != null) GroupDialog.show(owner,group).ifPresent(request -> FxTasks.run(() -> api.updateGroup(group.id(),request), ignored -> load(), error -> Ui.error(owner,error)));
    }

    private void toggle() {
        Models.Group group = table.getSelectionModel().getSelectedItem();
        if (group == null) return;
        FxTasks.run(() -> api.setGroupActive(group.id(),!group.active(),group.version()), ignored -> load(), error -> Ui.error(owner,error));
    }
}
