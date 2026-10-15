package ru.mirea.studenthub.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.List;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.util.UiText;

/** Аналитическая сводка по студентам. */
public final class DashboardActivity extends SecureActivity {
    private ProgressBar progress;
    private TextView content;

    @Override
    protected void onCreate(@Nullable Bundle state) {
        super.onCreate(state);
        if (!requireSession()) return;
        setContentView(R.layout.activity_dashboard);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        progress = findViewById(R.id.progress);
        content = findViewById(R.id.content);
        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        enqueue(
                api().secureApi().analytics(),
                data -> {
                    progress.setVisibility(View.GONE);
                    if (data == null) {
                        content.setText("Нет данных");
                        return;
                    }
                    content.setText(render(data));
                },
                failure -> {
                    progress.setVisibility(View.GONE);
                    errorDialog("Не удалось загрузить аналитику", failure);
                }
        );
    }

    private String render(ApiModels.Analytics data) {
        StringBuilder result = new StringBuilder();
        result.append("Всего студентов: ").append(data.total).append('\n');
        result.append("Активных: ").append(data.active).append('\n');
        result.append("В архиве: ").append(data.archived).append('\n');
        result.append("С задолженностью: ").append(data.withDebt).append('\n');
        result.append("Средний балл: ").append(UiText.grade(data.averageGrade)).append(" / 100\n\n");

        appendCounts(result, "По группам", data.byGroup, false, false);
        appendCounts(result, "По статусам", data.byStatus, true, false);
        appendCounts(result, "По риску", data.byRisk, false, true);
        return result.toString();
    }

    private void appendCounts(StringBuilder out,
                              String title,
                              List<ApiModels.NamedCount> values,
                              boolean status,
                              boolean risk) {
        out.append(title).append(":\n");
        if (values == null || values.isEmpty()) {
            out.append("— нет данных\n\n");
            return;
        }
        for (ApiModels.NamedCount item : values) {
            String label = item.name;
            try {
                if (status) label = UiText.status(ApiModels.StudentStatus.valueOf(item.name));
                if (risk) label = UiText.risk(ApiModels.RiskLevel.valueOf(item.name));
            } catch (RuntimeException ignored) {
                // Неизвестное enum-значение отображается как исходный код без падения экрана.
            }
            out.append("• ").append(label).append(": ").append(item.count).append('\n');
        }
        out.append('\n');
    }
}
