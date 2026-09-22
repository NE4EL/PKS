package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.service.ConnectionRequestQueryService;
import ru.mirea.ispteam.service.ConnectionRequestService;
import ru.mirea.ispteam.util.InputReader;

/*
 * Владелец: C (UI). Скелет создан A.
 * TODO(C): подменю "Заявки" — список/создание/смена статуса/поиск/фильтр/сортировка
 * через ConnectionRequestService и ConnectionRequestQueryService.
 */
public class RequestMenu {

    private final ConnectionRequestService requestService;
    private final ConnectionRequestQueryService queryService;
    private final InputReader input;

    public RequestMenu(ConnectionRequestService requestService,
                       ConnectionRequestQueryService queryService,
                       InputReader input) {
        this.requestService = requestService;
        this.queryService = queryService;
        this.input = input;
    }

    public void show() {
        throw new UnsupportedOperationException("TODO(C): реализовать меню заявок");
    }
}
