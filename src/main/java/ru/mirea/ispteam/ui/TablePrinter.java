package ru.mirea.ispteam.ui;

import java.util.List;

/*
 * Владелец: C (UI).
 * Печать выровненной таблицы в консоль:
 *
 *   +----+----------------------+
 *   | ID | ФИО                  |
 *   +----+----------------------+
 *   | 1  | Иванов Иван Иванович |
 *   +----+----------------------+
 *
 * Ширина колонки = самое длинное значение в ней (но не больше MAX_WIDTH —
 * длинные значения обрезаются с «…», чтобы таблица не разъезжалась).
 */
final class TablePrinter {

    private static final int MAX_WIDTH = 45;

    private TablePrinter() {
    }

    static void print(String[] headers, List<String[]> rows) {
        print(headers, rows, true);
    }

    // showCount = false — без строки "Записей: N" (например, для таблиц статистики).
    static void print(String[] headers, List<String[]> rows, boolean showCount) {
        if (rows.isEmpty()) {
            System.out.println("(нет записей)");
            return;
        }

        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                widths[i] = Math.max(widths[i], Math.min(text(row[i]).length(), MAX_WIDTH));
            }
        }

        String separator = separator(widths);
        System.out.println(separator);
        printRow(headers, widths);
        System.out.println(separator);
        for (String[] row : rows) {
            printRow(row, widths);
        }
        System.out.println(separator);
        if (showCount) {
            System.out.println("Записей: " + rows.size());
        }
    }

    private static void printRow(String[] cells, int[] widths) {
        StringBuilder sb = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String value = fit(text(cells[i]), widths[i]);
            sb.append(' ').append(value).append(" ".repeat(widths[i] - value.length())).append(" |");
        }
        System.out.println(sb);
    }

    private static String separator(int[] widths) {
        StringBuilder sb = new StringBuilder("+");
        for (int width : widths) {
            sb.append("-".repeat(width + 2)).append('+');
        }
        return sb.toString();
    }

    private static String fit(String value, int width) {
        return value.length() <= width ? value : value.substring(0, width - 1) + "…";
    }

    private static String text(String value) {
        return value == null ? "—" : value;
    }
}
