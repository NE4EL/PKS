package ru.mirea.studenthub.android.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.util.PhoneFormatter;
import ru.mirea.studenthub.android.util.PhoneMask;
import ru.mirea.studenthub.android.util.UiText;

/** Создание и редактирование карточки студента. */
public final class StudentEditActivity extends SecureActivity {
    public static final String EXTRA_STUDENT_ID = "student_id";

    private EditText lastName;
    private EditText firstName;
    private EditText middleName;
    private EditText externalId;
    private EditText email;
    private EditText phone;
    private EditText birthDate;
    private EditText averageGrade;
    private EditText tags;
    private Spinner group;
    private Spinner status;
    private CheckBox hasDebt;
    private ProgressBar progress;
    private View save;

    private final List<ApiModels.Group> groups = new ArrayList<>();
    private ApiModels.Student student;
    private String studentId;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!requireSession()) return;
        if (!currentUser().canEdit()) {
            message("Недостаточно прав для изменения студентов");
            finish();
            return;
        }
        setContentView(R.layout.activity_student_edit);

        studentId = getIntent().getStringExtra(EXTRA_STUDENT_ID);
        bindViews();
        configureControls();
        loadGroups();
    }

    private void bindViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setTitle(studentId == null ? "Новый студент" : "Изменение студента");

        lastName = findViewById(R.id.lastName);
        firstName = findViewById(R.id.firstName);
        middleName = findViewById(R.id.middleName);
        externalId = findViewById(R.id.externalId);
        email = findViewById(R.id.email);
        phone = findViewById(R.id.phone);
        birthDate = findViewById(R.id.birthDate);
        group = findViewById(R.id.group);
        status = findViewById(R.id.status);
        averageGrade = findViewById(R.id.averageGrade);
        hasDebt = findViewById(R.id.hasDebt);
        tags = findViewById(R.id.tags);
        save = findViewById(R.id.save);
        progress = findViewById(R.id.progress);
    }

    private void configureControls() {
        PhoneMask.attach(phone);
        phone.setHint("+7-999-999-99-99");
        averageGrade.setHint("Средний балл 0–100");

        ApiModels.StudentStatus[] statuses = ApiModels.StudentStatus.values();
        String[] statusLabels = Arrays.stream(statuses).map(UiText::status).toArray(String[]::new);
        status.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statusLabels));

        save.setOnClickListener(v -> save());
    }

    private void loadGroups() {
        setLoading(true);
        enqueue(
                api().secureApi().groups(false),
                result -> {
                    groups.clear();
                    if (result != null) groups.addAll(result);
                    List<String> labels = new ArrayList<>();
                    labels.add("Без группы");
                    for (ApiModels.Group item : groups) labels.add(UiText.group(item));
                    group.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));

                    if (studentId == null) {
                        setLoading(false);
                    } else {
                        loadStudent();
                    }
                },
                failure -> {
                    setLoading(false);
                    errorDialog("Не удалось загрузить группы", failure);
                }
        );
    }

    private void loadStudent() {
        enqueue(
                api().secureApi().student(studentId),
                result -> {
                    setLoading(false);
                    if (result == null) {
                        errorDialog("Студент", "Карточка не найдена");
                        return;
                    }
                    student = result;
                    populate(result);
                },
                failure -> {
                    setLoading(false);
                    errorDialog("Не удалось загрузить карточку", failure);
                }
        );
    }

    private void populate(ApiModels.Student value) {
        lastName.setText(value.lastName);
        firstName.setText(value.firstName);
        middleName.setText(value.middleName);
        externalId.setText(value.externalId);
        email.setText(value.email);
        phone.setText(value.phone == null || value.phone.trim().isEmpty() ? "" : PhoneFormatter.format(value.phone));
        birthDate.setText(value.birthDate);
        averageGrade.setText(value.averageGrade == null ? "" : UiText.grade(value.averageGrade));
        hasDebt.setChecked(value.hasDebt);
        tags.setText(value.tags == null ? "" : TextUtils.join(", ", value.tags));

        int statusPosition = Arrays.asList(ApiModels.StudentStatus.values()).indexOf(value.status);
        if (statusPosition >= 0) status.setSelection(statusPosition);

        if (value.groupId != null) {
            for (int i = 0; i < groups.size(); i++) {
                if (value.groupId.equals(groups.get(i).id)) {
                    group.setSelection(i + 1);
                    break;
                }
            }
        }
    }

    private void save() {
        ApiModels.StudentRequest request = buildRequest();
        if (request == null) return;

        setLoading(true);
        if (studentId == null) {
            enqueue(
                    api().secureApi().createStudent(request),
                    saved -> {
                        setLoading(false);
                        message("Карточка студента создана");
                        finish();
                    },
                    failure -> {
                        setLoading(false);
                        errorDialog("Не удалось сохранить студента", failure);
                    }
            );
        } else {
            enqueue(
                    api().secureApi().updateStudent(studentId, request),
                    saved -> {
                        setLoading(false);
                        message("Карточка студента обновлена");
                        finish();
                    },
                    failure -> {
                        setLoading(false);
                        errorDialog("Не удалось сохранить студента", failure);
                    }
            );
        }
    }

    @Nullable
    private ApiModels.StudentRequest buildRequest() {
        String last = value(lastName).trim();
        String first = value(firstName).trim();
        String emailValue = value(email).trim();
        String phoneValue = value(phone).trim();
        String birth = value(birthDate).trim();
        String gradeText = value(averageGrade).trim().replace(',', '.');

        if (last.trim().isEmpty()) {
            lastName.setError("Введите фамилию");
            lastName.requestFocus();
            return null;
        }
        if (first.trim().isEmpty()) {
            firstName.setError("Введите имя");
            firstName.requestFocus();
            return null;
        }
        if (!emailValue.trim().isEmpty() && !Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            email.setError("Некорректный email");
            email.requestFocus();
            return null;
        }
        if (!phoneValue.trim().isEmpty() && !PhoneFormatter.isComplete(phoneValue)) {
            phone.setError("Формат: +7-999-999-99-99");
            phone.requestFocus();
            return null;
        }
        if (!birth.trim().isEmpty()) {
            try {
                LocalDate date = LocalDate.parse(birth);
                if (!date.isBefore(LocalDate.now())) {
                    birthDate.setError("Дата рождения должна быть в прошлом");
                    return null;
                }
            } catch (DateTimeParseException ex) {
                birthDate.setError("Формат даты: YYYY-MM-DD");
                return null;
            }
        }

        BigDecimal grade = null;
        if (!gradeText.trim().isEmpty()) {
            try {
                grade = new BigDecimal(gradeText);
                if (grade.compareTo(BigDecimal.ZERO) < 0 || grade.compareTo(new BigDecimal("100")) > 0) {
                    averageGrade.setError("Допустимо от 0 до 100");
                    averageGrade.requestFocus();
                    return null;
                }
            } catch (NumberFormatException ex) {
                averageGrade.setError("Введите число от 0 до 100");
                return null;
            }
        }

        ApiModels.StudentRequest request = new ApiModels.StudentRequest();
        request.lastName = last;
        request.firstName = first;
        request.middleName = nullable(value(middleName));
        request.externalId = nullable(value(externalId));
        request.email = nullable(emailValue);
        request.phone = nullable(phoneValue);
        request.birthDate = nullable(birth);
        request.groupId = group.getSelectedItemPosition() <= 0
                ? null
                : groups.get(group.getSelectedItemPosition() - 1).id;
        request.status = ApiModels.StudentStatus.values()[status.getSelectedItemPosition()];
        request.averageGrade = grade;
        request.hasDebt = hasDebt.isChecked();
        request.tags = parseTags(value(tags));
        request.version = student == null ? null : student.version;
        return request;
    }

    private static LinkedHashSet<String> parseTags(String raw) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String part : raw.split(",")) {
            String value = part.trim();
            if (!value.trim().isEmpty()) result.add(value);
        }
        return result;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        save.setEnabled(!loading);
    }

    private static String nullable(String value) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.trim().isEmpty() ? null : trimmed;
    }

    private static String value(TextView view) {
        return view.getText() == null ? "" : view.getText().toString();
    }
}
