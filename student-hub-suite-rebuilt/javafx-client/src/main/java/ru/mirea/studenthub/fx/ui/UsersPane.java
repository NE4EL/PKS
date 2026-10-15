package ru.mirea.studenthub.fx.ui;

import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;
import ru.mirea.studenthub.fx.util.Ui;

/**
 * Административный экран пользователей и ролей.
 */
public final class UsersPane extends BorderPane {
    private final ApiClient api;
    private final Window owner;
    private final TableView<Models.User> table = new TableView<>();
    private final Button edit = new Button("Изменить");
    private final Button reset = new Button("Сбросить пароль");

    public UsersPane(ApiClient api, Window owner) {
        this.api = api;
        this.owner = owner;
        setPadding(new Insets(16));
        setTop(buildToolbar());
        configureTable();
        setCenter(table);
        load();
    }

    private ToolBar buildToolbar() {
        Button refresh = new Button("Обновить");
        refresh.setOnAction(e -> load());
        Button add = new Button("Добавить");
        add.getStyleClass().add("primary-button");
        add.setOnAction(e -> create());
        edit.setOnAction(e -> edit());
        reset.setOnAction(e -> resetPassword());
        return new ToolBar(refresh, add, edit, reset);
    }

    private void configureTable() {
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.getColumns().add(column("Email", Models.User::email));
        table.getColumns().add(column("Имя", Models.User::displayName));
        table.getColumns().add(column("Роли", UsersPane::roleLabels));
        TableColumn<Models.User, Boolean> enabled = new TableColumn<>("Активен");
        enabled.setCellValueFactory(d -> new ReadOnlyBooleanWrapper(d.getValue().enabled()));
        table.getColumns().add(enabled);
        TableColumn<Models.User, Boolean> temporary = new TableColumn<>("Временный пароль");
        temporary.setCellValueFactory(d -> new ReadOnlyBooleanWrapper(d.getValue().passwordChangeRequired()));
        table.getColumns().add(temporary);
        table.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            edit.setDisable(b == null);
            reset.setDisable(b == null);
        });
        edit.setDisable(true);
        reset.setDisable(true);
        table.setPlaceholder(Ui.emptyLabel("Пользователи не найдены"));
    }

    private void load() {
        table.setDisable(true);
        FxTasks.run(() -> api.users(0), page -> {
            table.setItems(FXCollections.observableArrayList(page.content()));
            table.setDisable(false);
        }, error -> {
            table.setDisable(false);
            Ui.error(owner, error);
        });
    }

    private void create() {
        UserDialog.show(owner, null).ifPresent(input ->
                FxTasks.run(
                        () -> api.createUser(
                                input.email(),
                                input.name(),
                                input.password(),
                                input.roles()
                        ),
                        ignored -> load(),
                        error -> Ui.error(owner, error)
                )
        );
    }

    private void edit() {
        Models.User user = table.getSelectionModel().getSelectedItem();
        if (user == null) return;

        UserDialog.show(owner, user).ifPresent(input ->
                FxTasks.run(
                        () -> api.updateUser(
                                user,
                                input.email(),
                                input.name(),
                                input.enabled(),
                                input.roles()
                        ),
                        ignored -> load(),
                        error -> Ui.error(owner, error)
                )
        );
    }

    private void resetPassword() {
        Models.User user = table.getSelectionModel().getSelectedItem();
        if (user == null) return;
        PasswordPrompt.show(owner, "Новый временный пароль").ifPresent(password -> FxTasks.run(() -> {
            api.resetUserPassword(user.id(), password);
            return null;
        }, ignored -> {
            Ui.info(owner, "Пароль сброшен");
            load();
        }, error -> Ui.error(owner, error)));
    }

    private static String roleLabels(Models.User user) {
        if (user.roles() == null || user.roles().isEmpty()) return "Без ролей";
        return user.roles().stream().map(Models.Role::toString).sorted().reduce((a, b) -> a + ", " + b).orElse("Без ролей");
    }

    private static TableColumn<Models.User, String> column(String title, java.util.function.Function<Models.User, String> mapper) {
        TableColumn<Models.User, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new ReadOnlyStringWrapper(mapper.apply(d.getValue())));
        return c;
    }
}
