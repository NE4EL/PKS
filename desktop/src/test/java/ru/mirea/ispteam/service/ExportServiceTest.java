package ru.mirea.ispteam.service;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.util.CsvExporter;
import ru.mirea.ispteam.util.ExcelExporter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportServiceTest {

    @TempDir
    Path dir;

    private ExportService prepare(TestData data) {
        Subscriber s = data.subscriber("Иванов Иван", "+79001112233");
        data.request(s, RequestType.TECHNICAL_SUPPORT, RequestStatus.NEW, null, "Описание; с \"кавычками\"",
                LocalDateTime.of(2024, 3, 5, 18, 0), LocalDateTime.of(2024, 3, 5, 18, 0));
        return new ExportService(data.subscribers, data.requests, new CsvExporter(), new ExcelExporter());
    }

    @Test
    void csvHasBomHeaderAndEscapedValues() throws Exception {
        TestData data = new TestData();
        ExportService export = prepare(data);
        Path file = dir.resolve("zayavki.csv");

        export.exportRequests(data.requests.getAll(), file);

        String text = Files.readString(file, StandardCharsets.UTF_8);
        assertThat(text).startsWith("﻿№ заявки;Абонент;Телефон;Тип;Статус");
        assertThat(text).contains("Иванов Иван;+79001112233;Техподдержка;Новая;;\"Описание; с \"\"кавычками\"\"\";05.03.2024 18:00");
        assertThat(text.lines()).hasSize(2);
    }

    @Test
    void xlsxContainsReadableRows() throws Exception {
        TestData data = new TestData();
        ExportService export = prepare(data);
        Path file = dir.resolve("abonenty.XLSX");

        export.exportSubscribers(data.subscribers.getAll(), file);

        try (InputStream in = Files.newInputStream(file); XSSFWorkbook book = new XSSFWorkbook(in)) {
            Sheet sheet = book.getSheet("Абоненты");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("ФИО");
            assertThat(sheet.getRow(1).getCell(0).getStringCellValue()).isEqualTo("Иванов Иван");
            assertThat(sheet.getRow(1).getCell(5).getStringCellValue()).isEqualTo("1");
            assertThat(sheet.getRow(1).getCell(6).getStringCellValue()).isEqualTo("1");
        }
    }

    @Test
    void unsupportedExtensionIsRejected() {
        TestData data = new TestData();
        ExportService export = prepare(data);
        assertThatThrownBy(() -> export.exportRequests(List.of(), dir.resolve("file.txt")))
                .isInstanceOf(ValidationException.class);
    }
}
