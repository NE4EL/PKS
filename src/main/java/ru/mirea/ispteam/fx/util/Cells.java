package ru.mirea.ispteam.fx.util;

import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.util.Callback;
import ru.mirea.ispteam.model.RequestStatus;

import java.util.function.Function;

/**
 * Фабрики ячеек TableView, общие для всех таблиц приложения.
 * Ячейка хранит исходное значение (дату, enum) — поэтому встроенная сортировка TableView
 * сравнивает именно значения, а на экран выводится отформатированный текст.
 */
public final class Cells {

    private Cells() {
    }

    /** Ячейка, которая показывает значение через функцию форматирования. */
    public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> formatted(Function<T, String> format) {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : format.apply(item));
            }
        };
    }

    /** Статус заявки в виде цветной «плашки» (цвет задаётся в CSS классом status-*). */
    public static <S> Callback<TableColumn<S, RequestStatus>, TableCell<S, RequestStatus>> statusBadge() {
        return column -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(RequestStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                    return;
                }
                badge.setText(status.getTitle());
                badge.getStyleClass().setAll("badge", statusStyle(status));
                setGraphic(badge);
            }
        };
    }

    public static String statusStyle(RequestStatus status) {
        return "status-" + status.name().toLowerCase().replace('_', '-');
    }
}
