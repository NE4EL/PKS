package ru.mirea.studenthub.fx.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import ru.mirea.studenthub.fx.model.Models;
import ru.mirea.studenthub.fx.net.ApiClient;
import ru.mirea.studenthub.fx.util.FxTasks;

/** Экран входа с неблокирующим сетевым вызовом. */
public final class LoginView extends StackPane {
    private final ApiClient api;
    private final Window owner;
    private final Runnable onSuccess;
    private final TextField email = new TextField();
    private final PasswordField password = new PasswordField();
    private final Button submit = new Button("Войти");
    private final Label error = new Label();
    private final ProgressIndicator progress = new ProgressIndicator();

    public LoginView(ApiClient api, Window owner, Runnable onSuccess) {
        this.api = api;
        this.owner = owner;
        this.onSuccess = onSuccess;
        getStyleClass().add("login-background");
        getChildren().add(buildCard());
    }

    private Region buildCard() {
        Label mark = new Label("SH");
        mark.getStyleClass().add("brand-mark");
        Label title = new Label("Student Hub");
        title.getStyleClass().add("title");
        Label subtitle = new Label("Desktop-клиент JavaFX");
        subtitle.getStyleClass().add("muted");

        email.setPromptText("admin@example.org");
        email.setMaxWidth(Double.MAX_VALUE);
        password.setPromptText("Пароль");
        password.setMaxWidth(Double.MAX_VALUE);
        password.setOnAction(event -> login());
        submit.getStyleClass().add("primary-button");
        submit.setMaxWidth(Double.MAX_VALUE);
        submit.setOnAction(event -> login());
        error.getStyleClass().add("error-label");
        error.setWrapText(true);
        error.setVisible(false);
        progress.setMaxSize(24, 24);
        progress.setVisible(false);

        VBox card = new VBox(14, mark, title, subtitle, new Label("Email"), email,
                new Label("Пароль"), password, submit, progress, error);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(32));
        card.setMaxWidth(420);
        card.getStyleClass().add("card");
        return card;
    }

    private void login() {
        if (email.getText().isBlank() || password.getText().isBlank()) {
            showError("Введите email и пароль");
            return;
        }
        setBusy(true);
        String suppliedPassword = password.getText();
        FxTasks.run(() -> api.login(email.getText(), suppliedPassword), response -> {
            // Поле пароля очищается сразу после получения ответа сервера.
            password.clear();
            setBusy(false);
            if (response.user().passwordChangeRequired()) {
                /*
                 * Временный пароль уже проверен сервером при login, поэтому диалог
                 * запрашивает только новый пароль и подтверждение.
                 */
                if (PasswordDialog.showInitial(owner, api)) {
                    onSuccess.run();
                } else {
                    api.logout();
                    showError("Для продолжения необходимо установить постоянный пароль");
                    email.requestFocus();
                }
            } else {
                onSuccess.run();
            }
        }, throwable -> {
            password.clear();
            setBusy(false);
            showError(FxTasks.friendly(throwable));
        });
    }

    private void setBusy(boolean busy) {
        email.setDisable(busy);
        password.setDisable(busy);
        submit.setDisable(busy);
        progress.setVisible(busy);
        error.setVisible(false);
    }

    private void showError(String message) {
        error.setText(message);
        error.setVisible(true);
    }
}
