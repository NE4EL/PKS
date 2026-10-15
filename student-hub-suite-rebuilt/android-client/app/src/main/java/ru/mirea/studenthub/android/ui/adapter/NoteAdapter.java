package ru.mirea.studenthub.android.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ru.mirea.studenthub.android.R;
import ru.mirea.studenthub.android.model.ApiModels;

/** Adapter преподавательских заметок. Нажатие открывает редактирование. */
public final class NoteAdapter extends RecyclerView.Adapter<NoteAdapter.Holder> {
    private final List<ApiModels.Note> items = new ArrayList<>();
    private final Consumer<ApiModels.Note> click;

    public NoteAdapter(Consumer<ApiModels.Note> click) {
        this.click = click;
    }

    public void submit(List<ApiModels.Note> values) {
        items.clear();
        if (values != null) items.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        ApiModels.Note note = items.get(position);
        holder.meta.setText((note.pinned ? "📌 " : "")
                + (note.authorName == null ? "Автор" : note.authorName)
                + (note.createdAt == null ? "" : " · " + note.createdAt));
        holder.body.setText(note.body);
        holder.itemView.setOnClickListener(v -> click.accept(note));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final TextView meta;
        final TextView body;

        Holder(@NonNull View itemView) {
            super(itemView);
            meta = itemView.findViewById(R.id.meta);
            body = itemView.findViewById(R.id.body);
        }
    }
}
