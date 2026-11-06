package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;

import java.time.LocalDate;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * КР2 (D). Снимок статистики, рассчитанный {@link StatisticsService#buildReport} за один проход по данным.
 *
 * @param totalSubscribers           всего абонентов
 * @param totalRequests              всего заявок
 * @param activeRequests             активные заявки (NEW + IN_PROGRESS + APPROVED)
 * @param completedRequests          выполненные (COMPLETED)
 * @param cancelledOrRejected        отменённые и отклонённые (CANCELLED + REJECTED)
 * @param completedPercent           доля выполненных заявок от всех, %
 * @param avgRequestsPerSubscriber   среднее число заявок на одного абонента
 * @param avgProcessingHours         среднее время выполнения заявки (создана -> выполнена), часы
 * @param minProcessingHours         минимальное время выполнения, часы
 * @param maxProcessingHours         максимальное время выполнения, часы
 * @param mostPopularTariff          самый частый тарифный план (null, если тарифов нет)
 * @param byStatus                   количество заявок по каждому статусу
 * @param byType                     количество заявок по каждому типу
 * @param periodFrom                 начало выбранного периода (может быть null)
 * @param periodTo                   конец выбранного периода (может быть null)
 * @param requestsInPeriod           число заявок, созданных в выбранном периоде
 */
public record StatisticsReport(long totalSubscribers,
                               long totalRequests,
                               long activeRequests,
                               long completedRequests,
                               long cancelledOrRejected,
                               double completedPercent,
                               double avgRequestsPerSubscriber,
                               OptionalDouble avgProcessingHours,
                               OptionalDouble minProcessingHours,
                               OptionalDouble maxProcessingHours,
                               String mostPopularTariff,
                               Map<RequestStatus, Long> byStatus,
                               Map<RequestType, Long> byType,
                               LocalDate periodFrom,
                               LocalDate periodTo,
                               long requestsInPeriod) {
}
