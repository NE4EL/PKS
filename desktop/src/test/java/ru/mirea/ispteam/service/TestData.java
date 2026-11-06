package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.repository.ConnectionRequestRepository;
import ru.mirea.ispteam.repository.SubscriberRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Репозитории в памяти и готовый набор сервисов для тестов без PostgreSQL. */
final class TestData {

    final InMemorySubscribers subscriberRepository = new InMemorySubscribers();
    final InMemoryRequests requestRepository = new InMemoryRequests();
    final SubscriberService subscribers = new SubscriberService(subscriberRepository, requestRepository);
    final ConnectionRequestService requests = new ConnectionRequestService(requestRepository, subscriberRepository);

    Subscriber subscriber(String name, String phone) {
        return subscriberRepository.save(new Subscriber(name, phone, null, "г. Москва", null));
    }

    /** Заявка с произвольным статусом и датами — записывается напрямую в репозиторий. */
    ConnectionRequest request(Subscriber s, RequestType type, RequestStatus status, String tariff,
                              String description, LocalDateTime created, LocalDateTime updated) {
        return requestRepository.save(new ConnectionRequest(s.getId(), type, status, tariff, description, created, updated));
    }

    static final class InMemorySubscribers implements SubscriberRepository {
        private final Map<Long, Subscriber> data = new LinkedHashMap<>();
        private long next = 1;

        public Subscriber save(Subscriber s) { s.setId(next++); data.put(s.getId(), s); return s; }
        public Optional<Subscriber> findById(Long id) { return Optional.ofNullable(data.get(id)); }
        public List<Subscriber> findAll() { return new ArrayList<>(data.values()); }
        public Subscriber update(Subscriber s) { data.put(s.getId(), s); return s; }
        public void deleteById(Long id) { data.remove(id); }
    }

    static final class InMemoryRequests implements ConnectionRequestRepository {
        private final Map<Long, ConnectionRequest> data = new LinkedHashMap<>();
        private long next = 1;

        public ConnectionRequest save(ConnectionRequest r) { r.setId(next++); data.put(r.getId(), r); return r; }
        public Optional<ConnectionRequest> findById(Long id) { return Optional.ofNullable(data.get(id)); }
        public List<ConnectionRequest> findAll() { return new ArrayList<>(data.values()); }
        public ConnectionRequest update(ConnectionRequest r) { data.put(r.getId(), r); return r; }
        public void deleteById(Long id) { data.remove(id); }

        public List<ConnectionRequest> findBySubscriberId(Long id) {
            return data.values().stream().filter(r -> Objects.equals(r.getSubscriberId(), id)).toList();
        }
    }
}
