package ru.mirea.ispteam.model;

/*
 * Владелец: D (модель). Скелет создан A.
 * Эталон из PKS.md разд. 2.3 — состав значений менять только по согласованию команды.
 *
 * КР2: у каждого значения есть русская подпись (getTitle) — её показывает JavaFX-интерфейс.
 * В БД по-прежнему хранится name() (NEW, IN_PROGRESS, ...).
 */
public enum RequestStatus {
    NEW("Новая"),
    IN_PROGRESS("В работе"),
    APPROVED("Одобрена"),
    COMPLETED("Выполнена"),
    REJECTED("Отклонена"),
    CANCELLED("Отменена");

    private final String title;

    RequestStatus(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    // Активные заявки: ещё не завершены (бизнес-правила 4 и 5).
    public boolean isActive() {
        return this == NEW || this == IN_PROGRESS || this == APPROVED;
    }
}
