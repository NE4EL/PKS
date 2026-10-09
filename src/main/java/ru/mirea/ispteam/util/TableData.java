package ru.mirea.ispteam.util;

import java.util.List;

/**
 * КР2. Табличные данные для экспорта: заголовки столбцов и строки уже в текстовом виде.
 * Один и тот же TableData сохраняется и в .csv, и в .xlsx — формат файла не влияет на подготовку данных.
 */
public record TableData(String title, List<String> headers, List<List<String>> rows) {

    public TableData {
        headers = List.copyOf(headers);
        rows = List.copyOf(rows);
    }
}
