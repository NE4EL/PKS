package ru.mirea.studenthub.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;
import ru.mirea.studenthub.android.util.UiText;

/** RecyclerView adapter карточек студентов с безопасным множественным выбором. */
public final class StudentAdapter extends RecyclerView.Adapter<StudentAdapter.Holder> {
    private final List<ApiModels.Student> items = new ArrayList<>();
    private final Set<String> selectedIds = new HashSet<>();
    private final Consumer<ApiModels.Student> openStudent;
    private final Consumer<Set<String>> selectionChanged;
    private boolean selectionEnabled;

    public StudentAdapter(boolean selectionEnabled,
                          Consumer<ApiModels.Student> openStudent,
                          Consumer<Set<String>> selectionChanged) {
        this.selectionEnabled = selectionEnabled;
        this.openStudent = openStudent;
        this.selectionChanged = selectionChanged;
    }

    public void submit(List<ApiModels.Student> values) {
        items.clear();
        if (values != null) items.addAll(values);
        Set<String> currentIds = new HashSet<>();
        for (ApiModels.Student item : items) currentIds.add(item.id);
        selectedIds.retainAll(currentIds);
        notifyDataSetChanged();
        notifySelection();
    }

    public void setSelectionEnabled(boolean enabled) {
        selectionEnabled = enabled;
        if (!enabled) selectedIds.clear();
        notifyDataSetChanged();
        notifySelection();
    }

    public Set<String> selectedIds() {
        return Collections.unmodifiableSet(new HashSet<>(selectedIds));
    }

    public void clearSelection() {
        selectedIds.clear();
        notifyDataSetChanged();
        notifySelection();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_student, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ApiModels.Student student = items.get(position);
        holder.name.setText(student.fullName == null || student.fullName.trim().isEmpty()
                ? (safe(student.lastName) + " " + safe(student.firstName)).trim()
                : student.fullName);

        String meta = (student.groupCode == null ? "Без группы" : student.groupCode)
                + " · " + UiText.status(student.status)
                + " · " + UiText.grade(student.averageGrade) + "/100"
                + (student.hasDebt ? " · долг" : "")
                + (student.archived ? " · архив" : "");
        holder.meta.setText(meta);
        holder.risk.setText("Риск: " + UiText.risk(student.riskLevel));

        holder.selected.setVisibility(selectionEnabled ? View.VISIBLE : View.GONE);
        holder.selected.setOnCheckedChangeListener(null);
        holder.selected.setChecked(selectedIds.contains(student.id));
        holder.selected.setOnCheckedChangeListener((button, checked) -> {
            if (checked) selectedIds.add(student.id); else selectedIds.remove(student.id);
            notifySelection();
        });

        holder.itemView.setOnClickListener(v -> openStudent.accept(student));
        holder.itemView.setOnLongClickListener(v -> {
            if (!selectionEnabled) return false;
            holder.selected.setChecked(!holder.selected.isChecked());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void notifySelection() {
        selectionChanged.accept(selectedIds());
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final CheckBox selected;
        final TextView name;
        final TextView meta;
        final TextView risk;

        Holder(@NonNull View itemView) {
            super(itemView);
            selected = itemView.findViewById(R.id.selected);
            name = itemView.findViewById(R.id.name);
            meta = itemView.findViewById(R.id.meta);
            risk = itemView.findViewById(R.id.risk);
        }
    }
}
