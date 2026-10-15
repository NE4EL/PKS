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

/** Универсальный adapter простых справочников: группы, пользователи, аудит. */
public final class SimpleListAdapter<T> extends RecyclerView.Adapter<SimpleListAdapter<T>.Holder> {
    public static final class Item<T> {
        public final String title;
        public final String subtitle;
        public final T value;

        public Item(String title, String subtitle, T value) {
            this.title = title;
            this.subtitle = subtitle;
            this.value = value;
        }
    }

    private final List<Item<T>> items = new ArrayList<>();
    private final Consumer<T> click;

    public SimpleListAdapter(Consumer<T> click) {
        this.click = click;
    }

    public void submit(List<Item<T>> values) {
        items.clear();
        if (values != null) items.addAll(values);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_simple, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Item<T> item = items.get(position);
        holder.title.setText(item.title);
        holder.subtitle.setText(item.subtitle);
        holder.itemView.setOnClickListener(v -> {
            if (click != null) click.accept(item.value);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    final class Holder extends RecyclerView.ViewHolder {
        final TextView title;
        final TextView subtitle;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.title);
            subtitle = itemView.findViewById(R.id.subtitle);
        }
    }
}
