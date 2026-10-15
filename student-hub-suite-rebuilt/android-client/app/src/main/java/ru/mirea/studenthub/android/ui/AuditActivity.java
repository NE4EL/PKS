package ru.mirea.studenthub.android.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

import java.util.ArrayList;
import java.util.List;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.ui.adapter.SimpleListAdapter;
import ru.mirea.studenthub.android.util.UiText;

/** Административный read-only журнал аудита. */
public final class AuditActivity extends SecureActivity {
    private EditText search;
    private View progress;
    private SimpleListAdapter<ApiModels.Audit> adapter;

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
        toolbar.setTitle("Журнал аудита");
        toolbar.setNavigationOnClickListener(v -> finish());

        search = findViewById(R.id.search);
        search.setHint("Поиск по аудиту");
        progress = findViewById(R.id.progress);
        findViewById(R.id.add).setVisibility(View.GONE);
        findViewById(R.id.searchButton).setOnClickListener(v -> load());

        adapter = new SimpleListAdapter<>(null);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);
        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        String q = search.getText() == null ? "" : search.getText().toString().trim();
        enqueue(
                api().secureApi().audit(q, 0, 100, "occurred,desc"),
                page -> {
                    progress.setVisibility(View.GONE);
                    List<SimpleListAdapter.Item<ApiModels.Audit>> rows = new ArrayList<>();
                    if (page != null && page.content != null) {
                        for (ApiModels.Audit item : page.content) {
                            String actor = item.actorEmail == null ? "система" : item.actorEmail;
                            String subtitle = actor
                                    + (item.occurredAt == null ? "" : " · " + item.occurredAt)
                                    + "\n" + (item.summary == null ? "" : item.summary)
                                    + (item.correlationId == null ? "" : "\nID: " + item.correlationId);
                            rows.add(new SimpleListAdapter.Item<>(
                                    UiText.auditEvent(item.eventType),
                                    subtitle,
                                    item
                            ));
                        }
                    }
                    adapter.submit(rows);
                },
                failure -> {
                    progress.setVisibility(View.GONE);
                    errorDialog("Не удалось загрузить аудит", failure);
                }
        );
    }
}
