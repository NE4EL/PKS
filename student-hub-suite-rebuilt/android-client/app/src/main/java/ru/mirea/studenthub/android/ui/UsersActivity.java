package ru.mirea.studenthub.android.ui;

import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.Set;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.ui.adapter.SimpleListAdapter;
import ru.mirea.studenthub.android.util.PasswordValidator;
import ru.mirea.studenthub.android.util.UiText;

/** Административное управление учётными записями и ролями. */
public final class UsersActivity extends SecureActivity {
    private final List<ApiModels.User> users = new ArrayList<>();
    private SimpleListAdapter<ApiModels.User> adapter;
    private EditText search;
    private View progress;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!requireSession()) return;
        if (!currentUser().isAdmin()) {
            message("Раздел доступен только администратору");
            finish();
            return;
        }
        setContentView(R.layout.activity_simple_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Пользователи");
        toolbar.setNavigationOnClickListener(v -> finish());

        search = findViewById(R.id.search);
        search.setHint("Email или имя пользователя");
        progress = findViewById(R.id.progress);
        findViewById(R.id.searchButton).setOnClickListener(v -> renderFilter());
        findViewById(R.id.add).setOnClickListener(v -> editUser(null));

        adapter = new SimpleListAdapter<>(this::editUser);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        enqueue(
                api().secureApi().users(0, 100, "email,asc"),
                result -> {
                    progress.setVisibility(View.GONE);
                    users.clear();
                    if (result != null && result.content != null) users.addAll(result.content);
                    renderFilter();
                },
                failure -> {
                    progress.setVisibility(View.GONE);
                    errorDialog("Не удалось загрузить пользователей", failure);
                }
        );
    }

    private void renderFilter() {
        String q = text(search).trim().toLowerCase(Locale.ROOT);
        List<SimpleListAdapter.Item<ApiModels.User>> rows = users.stream()
                .filter(user -> q.trim().isEmpty()
                        || safe(user.email).toLowerCase(Locale.ROOT).contains(q)
                        || safe(user.displayName).toLowerCase(Locale.ROOT).contains(q))
                .map(user -> new SimpleListAdapter.Item<>(
                        user.displayName + " · " + user.email,
                        UiText.roles(user.roles)
                                + " · " + (user.enabled ? "активен" : "отключён")
                                + (user.passwordChangeRequired ? " · требуется смена пароля" : "")
                                + (user.lockedUntil == null ? "" : " · блокировка до " + user.lockedUntil),
                        user
                ))
                .collect(Collectors.toList());
        adapter.submit(rows);
    }

    private void editUser(@Nullable ApiModels.User existing) {
        View view = getLayoutInflater().inflate(R.layout.dialog_user, null, false);
        EditText email = view.findViewById(R.id.userEmail);
        EditText displayName = view.findViewById(R.id.userDisplayName);
        EditText temporaryPassword = view.findViewById(R.id.userTemporaryPassword);
        TextView passwordLabel = view.findViewById(R.id.userTemporaryPasswordLabel);
        CheckBox enabled = view.findViewById(R.id.userEnabled);
        CheckBox viewer = view.findViewById(R.id.roleViewer);
        CheckBox teacher = view.findViewById(R.id.roleTeacher);
        CheckBox admin = view.findViewById(R.id.roleAdmin);
        TextView error = view.findViewById(R.id.userError);

        if (existing == null) {
            enabled.setChecked(true);
            viewer.setChecked(true);
        } else {
            email.setText(existing.email);
            displayName.setText(existing.displayName);
            enabled.setChecked(existing.enabled);
            viewer.setChecked(existing.roles != null && existing.roles.contains(ApiModels.Role.VIEWER));
            teacher.setChecked(existing.roles != null && existing.roles.contains(ApiModels.Role.TEACHER));
            admin.setChecked(existing.roles != null && existing.roles.contains(ApiModels.Role.ADMIN));
            temporaryPassword.setVisibility(View.GONE);
            passwordLabel.setVisibility(View.GONE);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Новый пользователь" : "Изменение пользователя")
                .setView(view)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null);

        if (existing != null) builder.setNeutralButton("Сбросить пароль", null);

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String emailValue = text(email).trim();
                String nameValue = text(displayName).trim();
                Set<ApiModels.Role> roles = selectedRoles(viewer, teacher, admin);

                if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
                    showError(error, "Введите корректный email");
                    return;
                }
                if (nameValue.trim().isEmpty()) {
                    showError(error, "Введите отображаемое имя");
                    return;
                }
                if (roles.isEmpty()) {
                    showError(error, "Выберите хотя бы одну роль");
                    return;
                }

                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                if (existing == null) {
                    String temp = text(temporaryPassword);
                    String validation = PasswordValidator.validate(temp, emailValue);
                    if (validation != null) {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        showError(error, validation);
                        return;
                    }
                    ApiModels.CreateUserRequest request = new ApiModels.CreateUserRequest();
                    request.email = emailValue;
                    request.displayName = nameValue;
                    request.temporaryPassword = temp;
                    request.roles = roles;
                    enqueue(api().secureApi().createUser(request), saved -> {
                        dialog.dismiss();
                        load();
                    }, failure -> {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        showError(error, failure);
                    });
                } else {
                    ApiModels.UpdateUserRequest request = new ApiModels.UpdateUserRequest();
                    request.email = emailValue;
                    request.displayName = nameValue;
                    request.enabled = enabled.isChecked();
                    request.roles = roles;
                    request.version = existing.version;
                    enqueue(api().secureApi().updateUser(existing.id, request), saved -> {
                        dialog.dismiss();
                        load();
                    }, failure -> {
                        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                        showError(error, failure);
                    });
                }
            });

            if (existing != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                        resetPassword(existing));
            }
        });
        dialog.show();
    }

    private void resetPassword(ApiModels.User user) {
        View view = getLayoutInflater().inflate(R.layout.dialog_reset_password, null, false);
        EditText password = view.findViewById(R.id.resetPassword);
        EditText confirm = view.findViewById(R.id.resetPasswordConfirm);
        TextView error = view.findViewById(R.id.resetPasswordError);

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle("Сброс пароля")
                .setMessage(user.displayName + " · " + user.email)
                .setView(view)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сбросить", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String value = text(password);
                    String validation = PasswordValidator.validate(value, user.email);
                    if (validation != null) {
                        showError(error, validation);
                        return;
                    }
                    if (!value.equals(text(confirm))) {
                        showError(error, "Пароли не совпадают");
                        return;
                    }
                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    enqueue(
                            api().secureApi().resetUserPassword(user.id, new ApiModels.ResetPasswordRequest(value)),
                            unused -> {
                                dialog.dismiss();
                                message("Временный пароль установлен");
                                load();
                            },
                            failure -> {
                                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                                showError(error, failure);
                            }
                    );
                }));
        dialog.show();
    }

    private static Set<ApiModels.Role> selectedRoles(CheckBox viewer, CheckBox teacher, CheckBox admin) {
        EnumSet<ApiModels.Role> result = EnumSet.noneOf(ApiModels.Role.class);
        if (viewer.isChecked()) result.add(ApiModels.Role.VIEWER);
        if (teacher.isChecked()) result.add(ApiModels.Role.TEACHER);
        if (admin.isChecked()) result.add(ApiModels.Role.ADMIN);
        return result;
    }

    private static void showError(TextView view, String text) {
        view.setText(text);
        view.setVisibility(View.VISIBLE);
    }

    private static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
