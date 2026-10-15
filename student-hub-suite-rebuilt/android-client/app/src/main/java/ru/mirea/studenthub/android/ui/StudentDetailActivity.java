package ru.mirea.studenthub.android.ui;

import android.content.Intent;
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

import java.util.List;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.ui.adapter.NoteAdapter;
import ru.mirea.studenthub.android.util.UiText;

/** Карточка студента, архивирование и CRUD преподавательских заметок. */
public final class StudentDetailActivity extends SecureActivity {
    public static final String EXTRA_STUDENT_ID = "student_id";

    private String studentId;
    private ApiModels.Student student;
    private TextView details;
    private View edit;
    private TextView archive;
    private View permanentDelete;
    private View addNote;
    private View notesSection;
    private NoteAdapter noteAdapter;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!requireSession()) return;
        setContentView(R.layout.activity_student_detail);

        studentId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        if (studentId == null || studentId.trim().isEmpty()) {
            message("Не указан идентификатор студента");
            finish();
            return;
        }

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        details = findViewById(R.id.details);
        edit = findViewById(R.id.edit);
        archive = findViewById(R.id.archive);
        permanentDelete = findViewById(R.id.permanentDelete);
        addNote = findViewById(R.id.addNote);
        notesSection = findViewById(R.id.notesSection);

        noteAdapter = new NoteAdapter(this::editNote);
        RecyclerView notes = findViewById(R.id.notes);
        notes.setLayoutManager(new LinearLayoutManager(this));
        notes.setAdapter(noteAdapter);

        edit.setVisibility(currentUser().canEdit() ? View.VISIBLE : View.GONE);
        archive.setVisibility(currentUser().canEdit() ? View.VISIBLE : View.GONE);
        addNote.setVisibility(currentUser().canEdit() ? View.VISIBLE : View.GONE);
        notesSection.setVisibility(currentUser().canEdit() ? View.VISIBLE : View.GONE);
        permanentDelete.setVisibility(View.GONE);

        edit.setOnClickListener(v -> {
            Intent intent = new Intent(this, StudentEditActivity.class);
            intent.putExtra(StudentEditActivity.EXTRA_STUDENT_ID, studentId);
            startActivity(intent);
        });
        archive.setOnClickListener(v -> toggleArchive());
        permanentDelete.setOnClickListener(v -> permanentDelete());
        addNote.setOnClickListener(v -> editNote(null));
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (api().session().hasAccessToken()) loadStudent();
    }

    private void loadStudent() {
        enqueue(
                api().secureApi().student(studentId),
                result -> {
                    student = result;
                    if (student == null) {
                        errorDialog("Студент", "Карточка не найдена");
                        return;
                    }
                    renderStudent();
                    if (currentUser().canEdit()) loadNotes();
                },
                failure -> errorDialog("Не удалось загрузить карточку", failure)
        );
    }

    private void renderStudent() {
        StringBuilder text = new StringBuilder();
        text.append(student.fullName == null ? "—" : student.fullName).append("\n\n");
        append(text, "Группа", student.groupCode == null ? "Без группы" : student.groupCode);
        append(text, "Статус", UiText.status(student.status));
        append(text, "Средний балл", UiText.grade(student.averageGrade) + "/100");
        append(text, "Задолженность", UiText.yesNo(student.hasDebt));
        append(text, "Риск", UiText.risk(student.riskLevel));
        append(text, "Email", student.email);
        append(text, "Телефон", student.phone);
        append(text, "Дата рождения", student.birthDate);
        append(text, "External ID", student.externalId);
        append(text, "Теги", student.tags == null || student.tags.isEmpty() ? "—" : String.join(", ", student.tags));
        append(text, "Архив", UiText.yesNo(student.archived));
        append(text, "Обновлено", student.updatedAt);
        details.setText(text.toString());

        archive.setText(student.archived ? "Восстановить" : "В архив");
        permanentDelete.setVisibility(currentUser().isAdmin() && student.archived ? View.VISIBLE : View.GONE);
    }

    private void toggleArchive() {
        if (student == null) return;
        String title = student.archived ? "Восстановить карточку?" : "Переместить карточку в архив?";
        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setMessage("Операция будет записана в журнал аудита.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton(student.archived ? "Восстановить" : "В архив", (d, w) -> {
                    if (student.archived) {
                        enqueue(api().secureApi().restoreStudent(studentId), value -> {
                            student = value;
                            renderStudent();
                            message("Карточка восстановлена");
                        }, failure -> errorDialog("Операция не выполнена", failure));
                    } else {
                        enqueue(api().secureApi().archiveStudent(studentId), value -> {
                            student = value;
                            renderStudent();
                            message("Карточка перемещена в архив");
                        }, failure -> errorDialog("Операция не выполнена", failure));
                    }
                })
                .show();
    }

    private void permanentDelete() {
        if (!currentUser().isAdmin() || student == null || !student.archived) return;
        new MaterialAlertDialogBuilder(this)
                .setTitle("Удалить навсегда?")
                .setMessage("Это необратимая административная операция. Используйте её только для ошибочно созданных данных.")
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Удалить", (d, w) -> enqueue(
                        api().secureApi().permanentDeleteStudent(studentId),
                        unused -> {
                            message("Карточка удалена");
                            finish();
                        },
                        failure -> errorDialog("Удаление не выполнено", failure)
                ))
                .show();
    }

    private void loadNotes() {
        enqueue(
                api().secureApi().notes(studentId),
                notes -> noteAdapter.submit(notes),
                failure -> errorDialog("Не удалось загрузить заметки", failure)
        );
    }

    /** Один диалог обслуживает создание, изменение и удаление заметки. */
    private void editNote(@Nullable ApiModels.Note note) {
        View view = getLayoutInflater().inflate(R.layout.dialog_note, null, false);
        EditText body = view.findViewById(R.id.noteBody);
        CheckBox pinned = view.findViewById(R.id.notePinned);
        TextView error = view.findViewById(R.id.noteError);

        if (note != null) {
            body.setText(note.body);
            pinned.setChecked(note.pinned);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(note == null ? "Новая заметка" : "Изменение заметки")
                .setView(view)
                .setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null);

        if (note != null) {
            builder.setNeutralButton("Удалить", null);
        }

        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.setOnShowListener(ignored -> {
            dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                String text = body.getText() == null ? "" : body.getText().toString().trim();
                if (text.trim().isEmpty()) {
                    error.setText("Введите текст заметки");
                    error.setVisibility(View.VISIBLE);
                    return;
                }

                ApiModels.NoteRequest request = new ApiModels.NoteRequest(
                        text,
                        pinned.isChecked(),
                        note == null ? null : note.version
                );

                if (note == null) {
                    enqueue(api().secureApi().createNote(studentId, request), saved -> {
                        dialog.dismiss();
                        loadNotes();
                    }, failure -> showError(error, failure));
                } else {
                    enqueue(api().secureApi().updateNote(studentId, note.id, request), saved -> {
                        dialog.dismiss();
                        loadNotes();
                    }, failure -> showError(error, failure));
                }
            });

            if (note != null) {
                dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v ->
                        new MaterialAlertDialogBuilder(this)
                                .setTitle("Удалить заметку?")
                                .setNegativeButton("Отмена", null)
                                .setPositiveButton("Удалить", (d, w) -> enqueue(
                                        api().secureApi().deleteNote(studentId, note.id),
                                        unused -> {
                                            dialog.dismiss();
                                            loadNotes();
                                        },
                                        failure -> showError(error, failure)
                                ))
                                .show());
            }
        });
        dialog.show();
    }

    private static void append(StringBuilder out, String label, @Nullable String value) {
        out.append(label).append(": ").append(value == null || value.trim().isEmpty() ? "—" : value).append('\n');
    }

    private static void showError(TextView view, String text) {
        view.setText(text);
        view.setVisibility(View.VISIBLE);
    }
}
