package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/*
 * Владелец: D (поиск/фильтрация/сортировка). Контракт — эталон из PKS.md разд. 4.
 */
public class ConnectionRequestQueryService {

    private final ConnectionRequestService requestService;
    private final SubscriberService subscriberService;

    public ConnectionRequestQueryService(ConnectionRequestService requestService,
                                          SubscriberService subscriberService) {
        this.requestService = requestService;
        this.subscriberService = subscriberService;
    }

    public List<ConnectionRequest> searchBySubscriber(String query) {
        String needle = query == null ? "" : query.toLowerCase();

        List<Long> matchingSubscriberIds = subscriberService.getAll().stream()
                .filter(subscriber -> matchesSubscriber(subscriber, needle))
                .map(Subscriber::getId)
                .collect(Collectors.toList());

        return requestService.getAll().stream()
                .filter(request -> matchingSubscriberIds.contains(request.getSubscriberId()))
                .collect(Collectors.toList());
    }

    public List<ConnectionRequest> searchByDescription(String keyword) {
        String needle = keyword == null ? "" : keyword.toLowerCase();

        return requestService.getAll().stream()
                .filter(request -> request.getDescription() != null
                        && request.getDescription().toLowerCase().contains(needle))
                .collect(Collectors.toList());
    }

    public List<ConnectionRequest> filterByStatus(RequestStatus status) {
        return requestService.getAll().stream()
                .filter(request -> request.getStatus() == status)
                .collect(Collectors.toList());
    }

    public List<ConnectionRequest> filterByType(RequestType type) {
        return requestService.getAll().stream()
                .filter(request -> request.getType() == type)
                .collect(Collectors.toList());
    }

    public List<ConnectionRequest> sortByCreatedAt(boolean ascending) {
        Comparator<ConnectionRequest> comparator = Comparator.comparing(ConnectionRequest::getCreatedAt);
        if (!ascending) {
            comparator = comparator.reversed();
        }

        return requestService.getAll().stream()
                .sorted(comparator)
                .collect(Collectors.toList());
    }

    public List<ConnectionRequest> sortByStatus() {
        return requestService.getAll().stream()
                .sorted(Comparator.comparing(ConnectionRequest::getStatus))
                .collect(Collectors.toList());
    }

    private boolean matchesSubscriber(Subscriber subscriber, String needle) {
        boolean nameMatches = subscriber.getFullName() != null
                && subscriber.getFullName().toLowerCase().contains(needle);
        boolean phoneMatches = subscriber.getPhone() != null
                && subscriber.getPhone().toLowerCase().contains(needle);
        return nameMatches || phoneMatches;
    }
}
