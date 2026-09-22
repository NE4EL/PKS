package ru.mirea.ispteam.util;

import ru.mirea.ispteam.model.ConnectionRequest;

import java.util.List;

/*
 * Владелец: D (экспорт). Скелет создан A.
 *
 * TODO(D): экспорт списка заявок в .xlsx через Apache POI (зависимость poi-ooxml уже в pom.xml).
 * Создать книгу, лист, шапку с названиями колонок, по строке на заявку, сохранить в файл.
 */
public class ExcelExporter {

    public void exportRequests(List<ConnectionRequest> requests, String filePath) {
        throw new UnsupportedOperationException("TODO(D): реализовать экспорт в .xlsx через Apache POI");
    }
}
