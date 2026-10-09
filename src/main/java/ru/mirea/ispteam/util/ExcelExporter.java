package ru.mirea.ispteam.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.ispteam.exception.ExportException;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/*
 * Владелец: D (экспорт).
 * Запись таблицы (TableData) в книгу Excel .xlsx через Apache POI:
 * жирная серая шапка, ширина столбцов по содержимому, закреплённая первая строка.
 */
public class ExcelExporter {

    // Ограничение ширины столбца (в 1/256 символа), чтобы длинные описания не растягивали лист.
    private static final int MAX_COLUMN_WIDTH = 60 * 256;

    public void export(TableData data, Path file) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream out = Files.newOutputStream(file)) {

            Sheet sheet = workbook.createSheet(data.title());
            writeHeaderRow(sheet, data.headers(), createHeaderStyle(workbook));

            int rowIndex = 1;
            for (List<String> values : data.rows()) {
                Row row = sheet.createRow(rowIndex++);
                for (int col = 0; col < values.size(); col++) {
                    row.createCell(col).setCellValue(values.get(col) != null ? values.get(col) : "");
                }
            }

            for (int col = 0; col < data.headers().size(); col++) {
                sheet.autoSizeColumn(col);
                if (sheet.getColumnWidth(col) > MAX_COLUMN_WIDTH) {
                    sheet.setColumnWidth(col, MAX_COLUMN_WIDTH);
                }
            }
            sheet.createFreezePane(0, 1);

            workbook.write(out);
        } catch (IOException e) {
            throw new ExportException("Не удалось записать файл " + file.getFileName()
                    + ". Проверьте, что он не открыт в другой программе.", e);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void writeHeaderRow(Sheet sheet, List<String> headers, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < headers.size(); col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(headers.get(col));
            cell.setCellStyle(headerStyle);
        }
    }
}
