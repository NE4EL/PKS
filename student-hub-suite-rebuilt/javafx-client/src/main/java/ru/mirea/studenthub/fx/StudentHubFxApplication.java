package ru.mirea.studenthub.fx;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.ui.LoginView;
import ru.mirea.studenthub.fx.ui.MainView;

/**
 * Точка входа JavaFX-клиента.
 *
 * <p>Один экземпляр {@code ApiClient} используется на протяжении всего сеанса,
 * поэтому состояние access/refresh не дублируется между экранами.</p>
 */
public class StudentHubFxApplication extends Application {
    private Stage stage;
    private ApiClient api;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        String endpoint = System.getProperty("studenthub.api",
                System.getenv().getOrDefault("STUDENTHUB_API", "http://localhost:8080/api"));
        this.api = new ApiClient(endpoint);
        stage.setTitle("Student Hub — JavaFX");
        stage.setMinWidth(980);
        stage.setMinHeight(680);
        stage.setOnCloseRequest(event -> {
            // Logout выполняется в отдельном потоке, чтобы не блокировать JavaFX Application Thread.
            Thread thread = new Thread(() -> {
                try { api.logout(); } catch (RuntimeException ignored) { }
            }, "student-hub-logout");
            thread.setDaemon(true);
            thread.start();
        });
        showLogin();
        stage.show();
    }

    /** Показывает экран входа и очищает визуальное состояние предыдущего пользователя. */
    public void showLogin() {
        LoginView login = new LoginView(api, stage, this::showMain);
        setScene(new Scene(login, 520, 600));
    }

    /** Показывает основной интерфейс после успешной аутентификации. */
    public void showMain() {
        MainView main = new MainView(api, stage, this::logoutAndShowLogin);
        setScene(new Scene(main, 1320, 820));
    }

    private void logoutAndShowLogin() {
        Thread thread = new Thread(() -> {
            try { api.logout(); } catch (RuntimeException ignored) { }
            Platform.runLater(this::showLogin);
        }, "student-hub-logout");
        thread.setDaemon(true);
        thread.start();
    }

    private void setScene(Scene scene) {
        var css = getClass().getResource("/ru/mirea/studenthub/fx/app.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setScene(scene);
        stage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
