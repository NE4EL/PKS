package ru.mirea.studenthub.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.ui.adapter.SimpleListAdapter;
import ru.mirea.studenthub.android.util.UiText;

/** Справочник учебных групп. Изменение доступно только ADMIN. */
public final class GroupsActivity extends SecureActivity {
    private final List<ApiModels.Group> groups = new ArrayList<>();
    private SimpleListAdapter<ApiModels.Group> adapter;
    private EditText search;
    private View progress;
    private View add;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!requireSession()) return;
        setContentView(R.layout.activity_simple_list);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Учебные группы");
        toolbar.setNavigationOnClickListener(v -> finish());

        search = findViewById(R.id.search);
        progress = findViewById(R.id.progress);
        add = findViewById(R.id.add);
        findViewById(R.id.searchButton).setOnClickListener(v -> renderFilter());
        search.setHint("Код или название группы");

        adapter = new SimpleListAdapter<>(group -> {
            if (currentUser().isAdmin()) editGroup(group);
        });
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        add.setVisibility(currentUser().isAdmin() ? View.VISIBLE : View.GONE);
        add.setOnClickListener(v -> editGroup(null));
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && api().session().hasAccessToken()) load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        enqueue(
                api().secureApi().groups(false),
                result -> {
                    progress.setVisibility(View.GONE);
                    groups.clear();
                    if (result != null) groups.addAll(result);
                    renderFilter();
                },
                failure -> {
                    progress.setVisibility(View.GONE);
                    errorDialog("Не удалось загрузить группы", failure);
                }
        );
    }

    private void renderFilter() {
        String q = text(search).trim().toLowerCase(Locale.ROOT);
        List<SimpleListAdapter.Item<ApiModels.Group>> rows = groups.stream()
                .filter(group -> q.trim().isEmpty()
                        || safe(group.code).toLowerCase(Locale.ROOT).contains(q)
                        || safe(group.name).toLowerCase(Locale.ROOT).contains(q))
                .map(group -> new SimpleListAdapter.Item<>(
                        group.code,
                        (group.name == null ? "" : group.name + " · ")
                                + (group.admissionYear == null ? "год не указан" : group.admissionYear)
                                + " · " + (group.active ? "активна" : "неактивна"),
                        group
                ))
                .collect(Collectors.toList());
        adapter.submit(rows);
    }

    private void editGroup(@Nullable ApiModels.Group existing) {
        View view = getLayoutInflater().inflate(R.layout.dialog_group, null, false);
        EditText code = view.findViewById(R.id.groupCode);
        EditText name = view.findViewById(R.id.groupName);
        EditText year = view.findViewById(R.id.groupYear);
        CheckBox active = view.findViewById(R.id.groupActive);
        TextView error = view.findViewById(R.id.groupError);

        if (existing != null) {
            code.setText(existing.code);
            name.setText(existing.name);
            year.setText(existing.admissionYear == null ? "" : String.valueOf(existing.admissionYear));
            active.setChecked(existing.active);
        } else {
            active.setChecked(true);
        }

        androidx.appcompat.app.AlertDialog dialog = new MaterialAlertDialogBuilder(this)
                .setTitle(existing == null ? "Новая группа" : "Изменение группы")
                .setView(view)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null)
                .create();

        dialog.setOnShowListener(ignored -> dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String codeValue = text(code).trim();
                    if (codeValue.trim().isEmpty()) {
                        showError(error, "Введите код группы");
                        return;
                    }

                    Integer admissionYear = null;
                    String yearValue = text(year).trim();
                    if (!yearValue.trim().isEmpty()) {
                        try {
                            admissionYear = Integer.parseInt(yearValue);
                            if (admissionYear < 2000 || admissionYear > 2100) {
                                showError(error, "Год поступления должен быть от 2000 до 2100");
                                return;
                            }
                        } catch (NumberFormatException ex) {
                            showError(error, "Некорректный год поступления");
                            return;
                        }
                    }

                    ApiModels.GroupRequest request = new ApiModels.GroupRequest();
                    request.code = codeValue;
                    request.name = nullable(text(name));
                    request.admissionYear = admissionYear;
                    request.active = active.isChecked();
                    request.version = existing == null ? null : existing.version;

                    dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                    if (existing == null) {
                        enqueue(api().secureApi().createGroup(request), saved -> {
                            dialog.dismiss();
                            load();
                        }, failure -> {
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            showError(error, failure);
                        });
                    } else {
                        enqueue(api().secureApi().updateGroup(existing.id, request), saved -> {
                            dialog.dismiss();
                            load();
                        }, failure -> {
                            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setEnabled(true);
                            showError(error, failure);
                        });
                    }
                }));
        dialog.show();
    }

    private static void showError(TextView view, String text) {
        view.setText(text);
        view.setVisibility(View.VISIBLE);
    }

    private static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }

    private static String nullable(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.trim().isEmpty() ? null : trimmed;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
