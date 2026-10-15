package ru.mirea.studenthub.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import retrofit2.Call;
import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.util.PasswordValidator;

/**
 * Launcher Activity: вход, восстановление native refresh-сессии и обязательная
 * первичная замена временного пароля.
 */
public final class LoginActivity extends SecureActivity {
    private TextInputEditText email;
    private TextInputEditText password;
    private MaterialButton loginButton;
    private ProgressBar progress;
    private TextView error;
    private boolean restoring;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        setContentView(R.layout.activity_login);

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        loginButton = findViewById(R.id.loginButton);
        progress = findViewById(R.id.progress);
        error = findViewById(R.id.error);

        // Для локального запуска подставляется только email; поле пароля остаётся пустым.
        email.setText("admin@example.org");
        loginButton.setOnClickListener(v -> login());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!restoring && !api().session().hasAccessToken() && api().session().hasRefreshToken()) {
            restoreSession();
        }
    }

    private void login() {
        hideError();
        String emailValue = text(email).trim();
        String passwordValue = text(password);

        if (emailValue.trim().isEmpty() || passwordValue.trim().isEmpty()) {
            showError("Введите email и пароль");
            return;
        }

        setLoading(true);
        enqueue(
                api().publicApi().login(new ApiModels.LoginRequest(emailValue, passwordValue)),
                result -> {
                    setLoading(false);
                    if (result == null || result.accessToken == null || result.user == null) {
                        showError("Сервер вернул неполный ответ авторизации");
                        return;
                    }
                    api().session().apply(result);
                    password.setText("");
                    continueAfterAuthentication();
                },
                failure -> {
                    setLoading(false);
                    showError(failure);
                }
        );
    }

    /** Восстанавливает сессию после перезапуска процесса только через refresh token. */
    private void restoreSession() {
        String refresh = api().session().refreshToken();
        if (refresh == null) return;

        restoring = true;
        setLoading(true);
        enqueue(
                api().publicApi().refresh(new ApiModels.RefreshRequest(refresh)),
                result -> {
                    restoring = false;
                    setLoading(false);
                    if (result == null || result.accessToken == null || result.user == null) {
                        api().session().clear();
                        return;
                    }
                    api().session().apply(result);
                    continueAfterAuthentication();
                },
                failure -> {
                    restoring = false;
                    setLoading(false);
                    api().session().clear();
                }
        );
    }

    private void continueAfterAuthentication() {
        ApiModels.CurrentUser user = api().session().user();
        if (user == null) {
            showError("Не удалось получить профиль пользователя");
            return;
        }

        if (user.passwordChangeRequired) {
            showInitialPasswordDialog();
            return;
        }
        openMain();
    }

    /**
     * При первичной смене текущий пароль повторно не запрашивается: он уже проверен
     * в {@code /auth/login}, а endpoint смены принимает новый пароль.
     */
    private void showInitialPasswordDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_change_password, null, false);
        TextInputLayout currentLayout = content.findViewById(R.id.currentPasswordLayout);
        TextInputEditText newPassword = content.findViewById(R.id.newPassword);
        TextInputEditText confirmPassword = content.findViewById(R.id.confirmPassword);
        TextView dialogError = content.findViewById(R.id.dialogError);
        currentLayout.setVisibility(View.GONE);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Установка постоянного пароля")
                .setMessage("Временный пароль необходимо заменить перед началом работы.")
                .setView(content)
                .setCancelable(false)
                .setPositiveButton("Установить", null)
                .setNegativeButton("Выйти", (d, which) -> logoutLocal())
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String next = text(newPassword);
                    String confirm = text(confirmPassword);
                    String validation = PasswordValidator.validate(next, api().session().user().email);
                    if (validation != null) {
                        dialogError.setText(validation);
                        dialogError.setVisibility(View.VISIBLE);
                        return;
                    }
                    if (!next.equals(confirm)) {
                        dialogError.setText("Новые пароли не совпадают");
                        dialogError.setVisibility(View.VISIBLE);
                        return;
                    }

                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    enqueue(
                            api().secureApi().changeInitialPassword(
                                    new ApiModels.InitialPasswordChangeRequest(next)
                            ),
                            unused -> {
                                // Профиль перечитывается после смены пароля, чтобы обновить
                                // локальный passwordChangeRequired без повторного входа.
                                enqueue(
                                        api().secureApi().me(),
                                        user -> {
                                            api().session().setUser(user);
                                            dialog.dismiss();
                                            openMain();
                                        },
                                        failure -> {
                                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                            dialogError.setText(failure);
                                            dialogError.setVisibility(View.VISIBLE);
                                        }
                                );
                            },
                            failure -> {
                                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                dialogError.setText(failure);
                                dialogError.setVisibility(View.VISIBLE);
                            }
                    );
                }));
        dialog.show();
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private void logoutLocal() {
        api().session().clear();
        password.setText("");
        showError("Сессия завершена");
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        loginButton.setEnabled(!loading);
        loginButton.setText(loading ? "Входим…" : getString(R.string.login));
        email.setEnabled(!loading);
        password.setEnabled(!loading);
    }

    private void showError(String text) {
        error.setText(text);
        error.setVisibility(View.VISIBLE);
    }

    private void hideError() {
        error.setVisibility(View.GONE);
        error.setText("");
    }

    private static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }
}
