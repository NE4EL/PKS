package ru.mirea.ispteam.model;

/*
 * Владелец: D (модель). Скелет создан A.
 * Эталон из PKS.md разд. 2.3 — состав значений менять только по согласованию команды.
 *
 * КР2: русская подпись (title) для отображения; в БД хранится name().
 */
public enum RequestType {
    NEW_CONNECTION("Подключение"),
    TARIFF_CHANGE("Смена тарифа"),
    TECHNICAL_SUPPORT("Техподдержка"),
    DISCONNECTION("Отключение"),
    EQUIPMENT_REPLACEMENT("Замена оборудования");

    private final String title;

    RequestType(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    // Тарифный план имеет смысл только для подключения и смены тарифа (PKS.md разд. 2.2).
    public boolean usesTariff() {
        return this == NEW_CONNECTION || this == TARIFF_CHANGE;
    }
}
