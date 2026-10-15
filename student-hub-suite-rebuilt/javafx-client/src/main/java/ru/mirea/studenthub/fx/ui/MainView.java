package ru.mirea.studenthub.fx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.Ui;

/** Основной desktop-интерфейс с вкладками по прикладным разделам. */
public final class MainView extends BorderPane {
    private final ApiClient api;
    private final Window owner;

    public MainView(ApiClient api, Window owner, Runnable logout) {
        this.api = api;
        this.owner = owner;
        getStyleClass().add("app-root");
        setTop(buildHeader(logout));
        setCenter(buildTabs());
    }

    private HBox buildHeader(Runnable logout) {
        Models.CurrentUser user = api.currentUser();
        Label title = new Label("Student Hub");
        title.getStyleClass().add("header-title");
        Label identity = new Label(user.displayName() + " · " + user.email() + " · " + roleLabels(user));
        identity.getStyleClass().add("muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button password = new Button("Сменить пароль");
        password.setOnAction(event -> {
            if (PasswordDialog.showRegular(owner, api)) {
                Ui.info(owner, "Пароль изменён. Выполните вход заново.");
                logout.run();
            }
        });
        Button exit = new Button("Выйти");
        exit.getStyleClass().add("danger-button");
        exit.setOnAction(event -> logout.run());
        HBox header = new HBox(12, title, identity, spacer, password, exit);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 20, 14, 20));
        header.getStyleClass().add("header");
        return header;
    }

    private TabPane buildTabs() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().add(tab("Аналитика", new DashboardPane(api, owner)));
        tabs.getTabs().add(tab("Студенты", new StudentsPane(api, owner)));
        tabs.getTabs().add(tab("Группы", new GroupsPane(api, owner)));
        if (api.currentUser().isAdmin()) {
            tabs.getTabs().add(tab("Пользователи", new UsersPane(api, owner)));
            tabs.getTabs().add(tab("Аудит", new AuditPane(api, owner)));
        }
        return tabs;
    }

    private static String roleLabels(Models.CurrentUser user) {
        if (user.roles() == null || user.roles().isEmpty()) return "Без ролей";
        return user.roles().stream().map(Models.Role::toString).sorted().reduce((a, b) -> a + ", " + b).orElse("Без ролей");
    }

    private static Tab tab(String title, javafx.scene.Node content) {
        Tab tab = new Tab(title, content);
        tab.setClosable(false);
        return tab;
    }
}
