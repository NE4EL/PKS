package ru.mirea.studenthub.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.util.PasswordValidator;
import ru.mirea.studenthub.android.util.UiText;

/** Главное ролевое меню Android-клиента. */
public final class MainActivity extends SecureActivity {
    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!requireSession()) return;
        setContentView(R.layout.activity_main);

        ApiModels.CurrentUser user = currentUser();
        TextView userInfo = findViewById(R.id.userInfo);
        userInfo.setText(user.displayName + "\n" + user.email + "\n" + UiText.roles(user.roles));

        findViewById(R.id.dashboardButton).setOnClickListener(v -> open(DashboardActivity.class));
        findViewById(R.id.studentsButton).setOnClickListener(v -> open(StudentsActivity.class));
        findViewById(R.id.groupsButton).setOnClickListener(v -> open(GroupsActivity.class));
        findViewById(R.id.usersButton).setOnClickListener(v -> open(UsersActivity.class));
        findViewById(R.id.auditButton).setOnClickListener(v -> open(AuditActivity.class));
        findViewById(R.id.changePasswordButton).setOnClickListener(v -> changePassword());
        findViewById(R.id.logoutButton).setOnClickListener(v -> logout());

        // Пользовательские и аудиторские разделы в backend закрыты hasRole('ADMIN').
        findViewById(R.id.usersButton).setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);
        findViewById(R.id.auditButton).setVisibility(user.isAdmin() ? View.VISIBLE : View.GONE);
    }

    private void open(Class<?> target) {
        startActivity(new Intent(this, target));
    }

    private void changePassword() {
        View content = getLayoutInflater().inflate(R.layout.dialog_change_password, null, false);
        TextInputEditText current = content.findViewById(R.id.currentPassword);
        TextInputEditText next = content.findViewById(R.id.newPassword);
        TextInputEditText confirm = content.findViewById(R.id.confirmPassword);
        TextView error = content.findViewById(R.id.dialogError);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Смена пароля")
                .setView(content)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Изменить", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String currentValue = value(current);
                    String nextValue = value(next);
                    String confirmValue = value(confirm);
                    String validation = PasswordValidator.validate(nextValue, currentUser().email);

                    if (currentValue.trim().isEmpty()) {
                        showDialogError(error, "Введите текущий пароль");
                        return;
                    }
                    if (validation != null) {
                        showDialogError(error, validation);
                        return;
                    }
                    if (!nextValue.equals(confirmValue)) {
                        showDialogError(error, "Новые пароли не совпадают");
                        return;
                    }

                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    enqueue(
                            api().secureApi().changePassword(
                                    new ApiModels.ChangePasswordRequest(currentValue, nextValue)
                            ),
                            unused -> {
                                dialog.dismiss();
                                // После смены пароля сервер отзывает refresh-сессии; локальные токены также очищаются.
                                api().session().clear();
                                Intent intent = new Intent(this, LoginActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            },
                            failure -> {
                                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                showDialogError(error, failure);
                            }
                    );
                }));
        dialog.show();
    }

    private void logout() {
        String refresh = api().session().refreshToken();
        if (refresh == null) {
            finishLogout();
            return;
        }

        enqueue(
                api().publicApi().logout(new ApiModels.RefreshRequest(refresh)),
                unused -> finishLogout(),
                failure -> finishLogout()
        );
    }

    private void finishLogout() {
        api().session().clear();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private static void showDialogError(TextView error, String value) {
        error.setText(value);
        error.setVisibility(View.VISIBLE);
    }

    private static String value(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }
}
