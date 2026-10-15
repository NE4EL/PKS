package ru.mirea.studenthub.android.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.ui.adapter.StudentAdapter;
import ru.mirea.studenthub.android.util.FileTransferUtil;
import ru.mirea.studenthub.android.util.UiText;

/**
 * Список студентов: умный серверный фильтр, пагинация, массовые операции, CSV
 * import/export и переход к карточке.
 */
public final class StudentsActivity extends SecureActivity {
    private static final int PAGE_SIZE = 25;

    private MaterialToolbar toolbar;
    private TextInputEditText search;
    private SwipeRefreshLayout swipe;
    private TextView pageInfo;
    private View previous;
    private View next;
    private FloatingActionButton add;
    private StudentAdapter adapter;

    private int page;
    private int totalPages;
    private String query = "";
    private ApiModels.ImportMode pendingImportMode = ApiModels.ImportMode.CREATE_ONLY;

    private ActivityResultLauncher<String[]> openCsv;
    private ActivityResultLauncher<String> createCsv;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!requireSession()) return;
        setContentView(R.layout.activity_students);

        toolbar = findViewById(R.id.toolbar);
        search = findViewById(R.id.search);
        swipe = findViewById(R.id.swipe);
        pageInfo = findViewById(R.id.pageInfo);
        previous = findViewById(R.id.previous);
        next = findViewById(R.id.next);
        add = findViewById(R.id.add);
        RecyclerView list = findViewById(R.id.list);

        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_students);
        configureMenu(toolbar.getMenu());
        toolbar.setOnMenuItemClickListener(this::onToolbarItem);

        search.setHint("Умный фильтр: Иванов группа:ЭФБО долг:да балл<60");
        findViewById(R.id.searchButton).setOnClickListener(v -> {
            query = text(search).trim();
            page = 0;
            load();
        });

        adapter = new StudentAdapter(
                currentUser().canEdit(),
                this::openStudent,
                selected -> toolbar.setSubtitle(selected.isEmpty()
                        ? null
                        : "Выбрано: " + selected.size())
        );
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        swipe.setOnRefreshListener(this::load);
        previous.setOnClickListener(v -> {
            if (page > 0) {
                page--;
                load();
            }
        });
        next.setOnClickListener(v -> {
            if (page + 1 < totalPages) {
                page++;
                load();
            }
        });

        add.setVisibility(currentUser().canEdit() ? View.VISIBLE : View.GONE);
        add.setOnClickListener(v -> startActivity(new Intent(this, StudentEditActivity.class)));

        openCsv = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri != null) previewImport(uri);
        });
        createCsv = registerForActivityResult(new ActivityResultContracts.CreateDocument("text/csv"), uri -> {
            if (uri != null) exportTo(uri);
        });

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null && api().session().hasAccessToken()) load();
    }

    private void configureMenu(Menu menu) {
        boolean editor = currentUser().canEdit();
        menu.findItem(R.id.action_bulk).setVisible(editor);
        menu.findItem(R.id.action_import).setVisible(editor);
        menu.findItem(R.id.action_export).setVisible(editor);
    }

    private boolean onToolbarItem(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            load();
            return true;
        }
        if (id == R.id.action_bulk) {
            showBulkMenu();
            return true;
        }
        if (id == R.id.action_import) {
            chooseImportMode();
            return true;
        }
        if (id == R.id.action_export) {
            createCsv.launch("students-" + LocalDate.now() + ".csv");
            return true;
        }
        return false;
    }

    private void load() {
        if (!api().session().hasAccessToken()) return;
        swipe.setRefreshing(true);
        enqueue(
                api().secureApi().students(query, page, PAGE_SIZE, "name,asc"),
                result -> {
                    swipe.setRefreshing(false);
                    if (result == null) return;
                    totalPages = result.totalPages;
                    page = result.page;
                    adapter.submit(result.content);
                    pageInfo.setText("Страница " + (page + 1) + " из " + Math.max(1, totalPages)
                            + " · записей " + result.totalElements);
                    previous.setEnabled(!result.first);
                    next.setEnabled(!result.last);
                },
                failure -> {
                    swipe.setRefreshing(false);
                    errorDialog("Не удалось загрузить студентов", failure);
                }
        );
    }

    private void openStudent(ApiModels.Student student) {
        Intent intent = new Intent(this, StudentDetailActivity.class);
        intent.putExtra(StudentDetailActivity.EXTRA_STUDENT_ID, student.id);
        startActivity(intent);
    }

    private void showBulkMenu() {
        Set<String> ids = adapter.selectedIds();
        if (ids.isEmpty()) {
            message("Выберите хотя бы одного студента");
            return;
        }

        String[] actions = {
                "В архив",
                "Восстановить",
                "Изменить статус",
                "Добавить тег",
                "Удалить тег"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Массовая операция")
                .setItems(actions, (dialog, which) -> {
                    switch (which) {
                        case 0 -> executeBulk(ids, ApiModels.BulkAction.ARCHIVE, null, null);
                        case 1 -> executeBulk(ids, ApiModels.BulkAction.RESTORE, null, null);
                        case 2 -> chooseBulkStatus(ids);
                        case 3 -> askBulkTag(ids, ApiModels.BulkAction.ADD_TAG, "Добавить тег");
                        case 4 -> askBulkTag(ids, ApiModels.BulkAction.REMOVE_TAG, "Удалить тег");
                        default -> { }
                    }
                })
                .show();
    }

    private void chooseBulkStatus(Set<String> ids) {
        ApiModels.StudentStatus[] statuses = ApiModels.StudentStatus.values();
        String[] labels = Arrays.stream(statuses).map(UiText::status).toArray(String[]::new);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Новый статус")
                .setItems(labels, (dialog, which) ->
                        executeBulk(ids, ApiModels.BulkAction.SET_STATUS, statuses[which], null))
                .show();
    }

    private void askBulkTag(Set<String> ids, ApiModels.BulkAction action, String title) {
        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint("Например, староста");
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, 0, pad, 0);

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setView(input)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Применить", (dialog, which) -> {
                    String tag = text(input).trim();
                    if (tag.trim().isEmpty()) {
                        message("Введите тег");
                        return;
                    }
                    executeBulk(ids, action, null, tag);
                })
                .show();
    }

    private void executeBulk(Set<String> ids,
                             ApiModels.BulkAction action,
                             @Nullable ApiModels.StudentStatus status,
                             @Nullable String tag) {
        ApiModels.BulkRequest request = new ApiModels.BulkRequest();
        request.ids = new LinkedHashSet<>(ids);
        request.action = action;
        request.status = status;
        request.tag = tag;

        swipe.setRefreshing(true);
        enqueue(
                api().secureApi().bulk(request),
                result -> {
                    swipe.setRefreshing(false);
                    adapter.clearSelection();
                    if (result == null) {
                        message("Операция завершена");
                    } else {
                        message("Успешно: " + result.succeeded
                                + (result.failures == null || result.failures.isEmpty()
                                ? ""
                                : "; ошибок: " + result.failures.size()));
                    }
                    load();
                },
                failure -> {
                    swipe.setRefreshing(false);
                    errorDialog("Массовая операция не выполнена", failure);
                }
        );
    }

    private void chooseImportMode() {
        String[] modes = {
                "Только новые записи",
                "Обновлять по External ID"
        };
        new MaterialAlertDialogBuilder(this)
                .setTitle("Режим импорта CSV")
                .setItems(modes, (dialog, which) -> {
                    pendingImportMode = which == 0
                            ? ApiModels.ImportMode.CREATE_ONLY
                            : ApiModels.ImportMode.UPSERT_BY_EXTERNAL_ID;
                    openCsv.launch(new String[]{"text/csv", "text/comma-separated-values", "text/plain"});
                })
                .show();
    }

    private void previewImport(Uri uri) {
        swipe.setRefreshing(true);
        new Thread(() -> {
            try {
                File file = FileTransferUtil.copyCsvToCache(this, uri);
                RequestBody body = RequestBody.create(MediaType.parse("text/csv"), file);
                MultipartBody.Part part = MultipartBody.Part.createFormData("file", "students.csv", body);
                runOnUiThread(() -> enqueue(
                        api().secureApi().previewImport(pendingImportMode, part),
                        preview -> {
                            swipe.setRefreshing(false);
                            showImportPreview(file, preview);
                        },
                        failure -> {
                            swipe.setRefreshing(false);
                            //noinspection ResultOfMethodCallIgnored
                            file.delete();
                            errorDialog("Ошибка проверки CSV", failure);
                        }
                ));
            } catch (IOException ex) {
                runOnUiThread(() -> {
                    swipe.setRefreshing(false);
                    errorDialog("Не удалось открыть CSV", ex.getMessage());
                });
            }
        }, "studenthub-csv-import").start();
    }

    private void showImportPreview(File file, ApiModels.ImportPreview preview) {
        if (preview == null) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
            errorDialog("Импорт CSV", "Сервер не вернул результат проверки");
            return;
        }

        StringBuilder text = new StringBuilder()
                .append("Строк: ").append(preview.totalRows)
                .append("\nКорректных: ").append(preview.validRows)
                .append("\nОшибочных: ").append(preview.invalidRows);

        if (preview.issues != null && !preview.issues.isEmpty()) {
            text.append("\n\nПервые ошибки:");
            preview.issues.stream().limit(10).forEach(issue -> text.append("\n• строка ")
                    .append(issue.row).append(": ")
                    .append(issue.field == null ? "" : issue.field + " — ")
                    .append(issue.message));
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle("Предварительная проверка CSV")
                .setMessage(text.toString())
                .setNegativeButton("Отмена", (d, w) -> {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                });

        if (preview.invalidRows == 0) {
            builder.setPositiveButton("Импортировать", (d, w) -> applyImport(file));
        } else {
            builder.setPositiveButton("Понятно", (d, w) -> {
                //noinspection ResultOfMethodCallIgnored
                file.delete();
            });
        }
        builder.show();
    }

    private void applyImport(File file) {
        swipe.setRefreshing(true);
        RequestBody body = RequestBody.create(MediaType.parse("text/csv"), file);
        MultipartBody.Part part = MultipartBody.Part.createFormData("file", "students.csv", body);
        enqueue(
                api().secureApi().applyImport(pendingImportMode, part),
                result -> {
                    swipe.setRefreshing(false);
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    if (result != null) {
                        message("Импорт завершён: создано " + result.created + ", обновлено " + result.updated);
                    } else {
                        message("Импорт завершён");
                    }
                    page = 0;
                    load();
                },
                failure -> {
                    swipe.setRefreshing(false);
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                    errorDialog("Импорт не выполнен", failure);
                }
        );
    }

    private void exportTo(Uri uri) {
        swipe.setRefreshing(true);
        enqueue(
                api().secureApi().exportStudents(query),
                body -> {
                    if (body == null) {
                        swipe.setRefreshing(false);
                        errorDialog("Экспорт", "Сервер не вернул CSV");
                        return;
                    }
                    new Thread(() -> {
                        try {
                            FileTransferUtil.writeResponse(this, uri, body);
                            runOnUiThread(() -> {
                                swipe.setRefreshing(false);
                                message("CSV сохранён");
                            });
                        } catch (IOException ex) {
                            runOnUiThread(() -> {
                                swipe.setRefreshing(false);
                                errorDialog("Экспорт не выполнен", ex.getMessage());
                            });
                        } finally {
                            body.close();
                        }
                    }, "studenthub-csv-export").start();
                },
                failure -> {
                    swipe.setRefreshing(false);
                    errorDialog("Экспорт не выполнен", failure);
                }
        );
    }

    private static String text(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }
}
