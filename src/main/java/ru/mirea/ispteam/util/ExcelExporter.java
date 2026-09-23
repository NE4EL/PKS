package ru.mirea.ispteam.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import ru.mirea.ispteam.model.ConnectionRequest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

/*
 * Владелец: D (экспорт).
 */
public class ExcelExporter {

    private static final String[] HEADERS = {
            "ID", "ID абонента", "Тип заявки", "Статус", "Тарифный план",
            "Описание", "Создана", "Обновлена"
    };

    private static final DateTimeFormatter DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public void exportRequests(List<ConnectionRequest> requests, String filePath) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             OutputStream out = new FileOutputStream(filePath)) {

            Sheet sheet = workbook.createSheet("Заявки");

            CellStyle headerStyle = createHeaderStyle(workbook);
            writeHeaderRow(sheet, headerStyle);

            int rowIndex = 1;
            for (ConnectionRequest request : requests) {
                writeRequestRow(sheet, rowIndex++, request);
            }

            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
        } catch (IOException e) {
            throw new RuntimeException("Не удалось экспортировать заявки в файл: " + filePath, e);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }

    private void writeHeaderRow(Sheet sheet, CellStyle headerStyle) {
        Row headerRow = sheet.createRow(0);
        for (int col = 0; col < HEADERS.length; col++) {
            Cell cell = headerRow.createCell(col);
            cell.setCellValue(HEADERS[col]);
            cell.setCellStyle(headerStyle);
        }
    }

    private void writeRequestRow(Sheet sheet, int rowIndex, ConnectionRequest request) {
        Row row = sheet.createRow(rowIndex);

        row.createCell(0).setCellValue(request.getId() != null ? request.getId() : 0);
        row.createCell(1).setCellValue(request.getSubscriberId() != null ? request.getSubscriberId() : 0);
        row.createCell(2).setCellValue(request.getType() != null ? request.getType().name() : "");
        row.createCell(3).setCellValue(request.getStatus() != null ? request.getStatus().name() : "");
        row.createCell(4).setCellValue(request.getTariffPlan() != null ? request.getTariffPlan() : "");
        row.createCell(5).setCellValue(request.getDescription() != null ? request.getDescription() : "");
        row.createCell(6).setCellValue(request.getCreatedAt() != null ? request.getCreatedAt().format(DATE_TIME_FORMAT) : "");
        row.createCell(7).setCellValue(request.getUpdatedAt() != null ? request.getUpdatedAt().format(DATE_TIME_FORMAT) : "");
    }
}
