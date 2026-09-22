package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.service.StatisticsService;
import ru.mirea.ispteam.util.InputReader;

/*
 * Владелец: C (UI). Скелет создан A.
 * TODO(C): подменю "Статистика" — вывод показателей из StatisticsService.
 */
public class StatisticsMenu {

    private final StatisticsService statisticsService;
    private final InputReader input;

    public StatisticsMenu(StatisticsService statisticsService, InputReader input) {
        this.statisticsService = statisticsService;
        this.input = input;
    }

    public void show() {
        throw new UnsupportedOperationException("TODO(C): реализовать меню статистики");
    }
}
