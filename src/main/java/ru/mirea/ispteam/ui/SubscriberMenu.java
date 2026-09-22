package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.service.SubscriberService;
import ru.mirea.ispteam.util.InputReader;

/*
 * Владелец: C (UI). Скелет создан A.
 * TODO(C): подменю "Абоненты" — список/создание/редактирование/удаление через SubscriberService.
 */
public class SubscriberMenu {

    private final SubscriberService subscriberService;
    private final InputReader input;

    public SubscriberMenu(SubscriberService subscriberService, InputReader input) {
        this.subscriberService = subscriberService;
        this.input = input;
    }

    public void show() {
        throw new UnsupportedOperationException("TODO(C): реализовать меню абонентов");
    }
}
