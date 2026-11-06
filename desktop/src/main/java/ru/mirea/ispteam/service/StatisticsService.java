package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;

import java.time.Duration;
import java.time.LocalDate;
import java.util.DoubleSummaryStatistics;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/*
 * Владелец: D (статистика).
 * buildReport() считает все показатели за один запрос к БД — их выводит окно статистики JavaFX.
 */
public class StatisticsService {

    private static final Set<RequestStatus> CANCELLED_OR_REJECTED_STATUSES =
            EnumSet.of(RequestStatus.CANCELLED, RequestStatus.REJECTED);

    private final SubscriberService subscriberService;
    private final ConnectionRequestService requestService;

    public StatisticsService(SubscriberService subscriberService,
                             ConnectionRequestService requestService) {
        this.subscriberService = subscriberService;
        this.requestService = requestService;
    }

    /**
     * Полный отчёт для окна статистики.
     *
     * @param from начало периода по дате создания (включительно), null — без ограничения
     * @param to   конец периода (включительно), null — без ограничения
     */
    public StatisticsReport buildReport(LocalDate from, LocalDate to) {
        RequestFilter period = new RequestFilter(null, null, null, from, to);
        period.validate();

        // Данные читаются из БД один раз — дальше все показатели считаются в памяти.
        long subscribers = subscriberService.getAll().size();
        List<ConnectionRequest> requests = requestService.getAll();
        long total = requests.size();
        long completed = countByStatusIn(requests, EnumSet.of(RequestStatus.COMPLETED));

        Map<RequestStatus, Long> byStatus = new EnumMap<>(RequestStatus.class);
        for (RequestStatus status : RequestStatus.values()) byStatus.put(status, 0L);
        requests.forEach(r -> byStatus.merge(r.getStatus(), 1L, Long::sum));

        Map<RequestType, Long> byType = new EnumMap<>(RequestType.class);
        for (RequestType type : RequestType.values()) byType.put(type, 0L);
        requests.forEach(r -> byType.merge(r.getType(), 1L, Long::sum));

        // Время выполнения: от создания до последнего изменения у выполненных заявок.
        DoubleSummaryStatistics hours = requests.stream()
                .filter(r -> r.getStatus() == RequestStatus.COMPLETED
                        && r.getCreatedAt() != null && r.getUpdatedAt() != null)
                .mapToDouble(r -> Duration.between(r.getCreatedAt(), r.getUpdatedAt()).toMinutes() / 60.0)
                .summaryStatistics();
        boolean hasCompleted = hours.getCount() > 0;

        String popularTariff = requests.stream()
                .map(ConnectionRequest::getTariffPlan)
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.<String, Long>comparingByValue().thenComparing(Map.Entry.comparingByKey()))
                .map(Map.Entry::getKey)
                .orElse(null);

        long inPeriod = requests.stream().filter(r -> period.matches(r, null)).count();

        return new StatisticsReport(
                subscribers,
                total,
                requests.stream().filter(r -> r.getStatus().isActive()).count(),
                completed,
                countByStatusIn(requests, CANCELLED_OR_REJECTED_STATUSES),
                total == 0 ? 0 : completed * 100.0 / total,
                subscribers == 0 ? 0 : (double) total / subscribers,
                hasCompleted ? OptionalDouble.of(hours.getAverage()) : OptionalDouble.empty(),
                hasCompleted ? OptionalDouble.of(hours.getMin()) : OptionalDouble.empty(),
                hasCompleted ? OptionalDouble.of(hours.getMax()) : OptionalDouble.empty(),
                popularTariff,
                byStatus,
                byType,
                from,
                to,
                inPeriod);
    }

    private static long countByStatusIn(List<ConnectionRequest> requests, Set<RequestStatus> statuses) {
        return requests.stream()
                .filter(request -> statuses.contains(request.getStatus()))
                .count();
    }
}
