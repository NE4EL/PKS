package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Владелец: D (статистика).
 */
public class StatisticsService {

    private static final Set<RequestStatus> ACTIVE_STATUSES =
            EnumSet.of(RequestStatus.NEW, RequestStatus.IN_PROGRESS, RequestStatus.APPROVED);

    private static final Set<RequestStatus> CANCELLED_OR_REJECTED_STATUSES =
            EnumSet.of(RequestStatus.CANCELLED, RequestStatus.REJECTED);

    private final SubscriberService subscriberService;
    private final ConnectionRequestService requestService;

    public StatisticsService(SubscriberService subscriberService,
                             ConnectionRequestService requestService) {
        this.subscriberService = subscriberService;
        this.requestService = requestService;
    }

    public long totalSubscribers() {
        return subscriberService.getAll().size();
    }

    public long totalRequests() {
        return requestService.getAll().size();
    }

    public long activeRequests() {
        return countByStatusIn(ACTIVE_STATUSES);
    }

    public long completedRequests() {
        return countByStatusIn(EnumSet.of(RequestStatus.COMPLETED));
    }

    public long cancelledOrRejectedRequests() {
        return countByStatusIn(CANCELLED_OR_REJECTED_STATUSES);
    }

    public Map<RequestType, Long> countByType() {
        return requestService.getAll().stream()
                .collect(Collectors.groupingBy(ConnectionRequest::getType, Collectors.counting()));
    }

    private long countByStatusIn(Set<RequestStatus> statuses) {
        List<ConnectionRequest> all = requestService.getAll();
        return all.stream()
                .filter(request -> statuses.contains(request.getStatus()))
                .count();
    }
}
