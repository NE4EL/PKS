package ru.mirea.ispteam.ui;

import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.service.StatisticsService;
import ru.mirea.ispteam.util.InputReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * Владелец: C (UI).
 * Подменю "Статистика" — вывод показателей из StatisticsService (D).
 * Все подсчёты делает сервис, здесь только форматирование.
 */
public class StatisticsMenu {

    private final StatisticsService statisticsService;
    private final InputReader input;

    public StatisticsMenu(StatisticsService statisticsService, InputReader input) {
        this.statisticsService = statisticsService;
        this.input = input;
    }

    public void show() {
        long totalSubscribers = statisticsService.totalSubscribers();
        long totalRequests = statisticsService.totalRequests();
        long active = statisticsService.activeRequests();
        long completed = statisticsService.completedRequests();
        long cancelledOrRejected = statisticsService.cancelledOrRejectedRequests();
        Map<RequestType, Long> byType = statisticsService.countByType();

        ConsoleHelper.clearScreen();
        ConsoleHelper.printHeader("СТАТИСТИКА");
        List<String[]> summary = new ArrayList<>();
        summary.add(new String[]{"Всего абонентов", String.valueOf(totalSubscribers), ""});
        summary.add(new String[]{"Всего заявок", String.valueOf(totalRequests), percent(totalRequests, totalRequests)});
        summary.add(new String[]{"Активных (новые, в работе, одобренные)", String.valueOf(active), percent(active, totalRequests)});
        summary.add(new String[]{"Выполненных", String.valueOf(completed), percent(completed, totalRequests)});
        summary.add(new String[]{"Отменённых и отклонённых", String.valueOf(cancelledOrRejected),
                percent(cancelledOrRejected, totalRequests)});
        TablePrinter.print(new String[]{"Показатель", "Значение", "Доля"}, summary, false);

        System.out.println();
        System.out.println("Заявки по типам:");
        List<String[]> types = new ArrayList<>();
        for (RequestType type : RequestType.values()) {
            long count = byType.getOrDefault(type, 0L);
            types.add(new String[]{Labels.type(type), String.valueOf(count), percent(count, totalRequests)});
        }
        TablePrinter.print(new String[]{"Тип заявки", "Количество", "Доля"}, types, false);

        input.waitForEnter();
    }

    private static String percent(long part, long total) {
        if (total == 0) {
            return "—";
        }
        return String.format("%.1f%%", part * 100.0 / total);
    }
}
