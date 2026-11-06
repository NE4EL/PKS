package ru.mirea.ispteam.util;

import ru.mirea.ispteam.exception.ExportException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

/*
 * КР2. Экспорт таблицы в .csv.
 * Разделитель ';' и BOM в начале файла — чтобы русский Excel сразу открыл файл по столбцам и в UTF-8.
 */
public class CsvExporter {

    private static final char SEPARATOR = ';';
    private static final String BOM = "﻿";

    public void export(TableData data, Path file) {
        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write(BOM);
            writeLine(writer, data.headers());
            for (List<String> row : data.rows()) {
                writeLine(writer, row);
            }
        } catch (IOException e) {
            throw new ExportException("Не удалось записать файл " + file.getFileName()
                    + ". Проверьте, что он не открыт в другой программе.", e);
        }
    }

    private void writeLine(BufferedWriter writer, List<String> cells) throws IOException {
        writer.write(cells.stream().map(CsvExporter::escape).collect(Collectors.joining(String.valueOf(SEPARATOR))));
        writer.write("\r\n");
    }

    // Значение с разделителем, кавычкой или переводом строки берётся в кавычки, кавычки удваиваются (RFC 4180).
    static String escape(String value) {
        if (value == null) return "";
        boolean needsQuotes = value.indexOf(SEPARATOR) >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        return needsQuotes ? '"' + value.replace("\"", "\"\"") + '"' : value;
    }
}
