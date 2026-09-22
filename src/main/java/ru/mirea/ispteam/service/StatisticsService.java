package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.RequestType;

import java.util.Map;

/*
 * Владелец: D (статистика). Скелет создан A.
 *
 * TODO(D): посчитать >= 5 показателей (PKS.md разд. 2.5) поверх сервисов B:
 *   - всего абонентов
 *   - всего заявок
 *   - активных заявок (NEW + IN_PROGRESS + APPROVED)
 *   - завершённых (COMPLETED)
 *   - отменённых/отклонённых (CANCELLED + REJECTED)
 *   - разбивка по RequestType
 */
public class StatisticsService {

    private final SubscriberService subscriberService;
    private final ConnectionRequestService requestService;

    public StatisticsService(SubscriberService subscriberService,
                             ConnectionRequestService requestService) {
        this.subscriberService = subscriberService;
        this.requestService = requestService;
    }

    public long totalSubscribers() {
        throw new UnsupportedOperationException("TODO(D): реализовать totalSubscribers");
    }

    public long totalRequests() {
        throw new UnsupportedOperationException("TODO(D): реализовать totalRequests");
    }

    public long activeRequests() {
        throw new UnsupportedOperationException("TODO(D): реализовать activeRequests");
    }

    public long completedRequests() {
        throw new UnsupportedOperationException("TODO(D): реализовать completedRequests");
    }

    public long cancelledOrRejectedRequests() {
        throw new UnsupportedOperationException("TODO(D): реализовать cancelledOrRejectedRequests");
    }

    public Map<RequestType, Long> countByType() {
        throw new UnsupportedOperationException("TODO(D): реализовать countByType");
    }
}
