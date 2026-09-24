package ru.mirea.ispteam.service;

import ru.mirea.ispteam.exception.*;
import ru.mirea.ispteam.model.*;
import ru.mirea.ispteam.repository.*;
import java.time.*;
import java.util.*;
import java.util.function.*;

/**
 * Автономные проверки B без БД и сторонних тестовых библиотек.
 * После mvn test-compile запуск в Windows:
 * java -cp "target/classes;target/test-classes" ru.mirea.ispteam.service.ServiceChecks
 */
public class ServiceChecks {
    private static int checks;
    private final Subscribers subscribers = new Subscribers();
    private final Requests requests = new Requests();
    private final SubscriberService ss = new SubscriberService(subscribers, requests);
    private final ConnectionRequestService rs = new ConnectionRequestService(requests, subscribers);

    public static void main(String[] args) {
        new ServiceChecks().crudAndValidation();
        transitions();
        duplicatesAndDeletion();
        System.out.println("PASS: " + checks + " service checks");
    }

    private static Subscriber subscriber(String phone) {
        return new Subscriber("Иванов Иван", phone, null, "Москва", null);
    }
    private static ConnectionRequest request(Long subscriberId) {
        return new ConnectionRequest(subscriberId, RequestType.NEW_CONNECTION, null,
                null, "Подключить интернет", null, null);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void fails(Class<? extends Throwable> type, Runnable action) {
        checks++;
        try { action.run(); }
        catch (Throwable error) {
            if (type.isInstance(error)) return;
            throw new AssertionError("Expected " + type + ", got " + error, error);
        }
        throw new AssertionError("Expected " + type);
    }

    private void crudAndValidation() {
        fails(ValidationException.class, () -> ss.create(null));
        for (String name : new String[]{null, " ", "Иван123"}) {
            Subscriber s = subscriber("+79991234567"); s.setFullName(name);
            fails(ValidationException.class, () -> ss.create(s));
        }
        for (String phone : new String[]{null, "", "89991234567", "+7999abcdefgh", "+799912345678"}) {
            fails(ValidationException.class, () -> ss.create(subscriber(phone)));
        }
        Subscriber invalid = subscriber("+79991234567"); invalid.setEmail("bad@");
        fails(ValidationException.class, () -> ss.create(invalid));
        invalid.setEmail(null); invalid.setAddress(" ");
        fails(ValidationException.class, () -> ss.create(invalid));
        Subscriber s = ss.create(subscriber("+79991234567"));
        check(s.getRegistrationDate().equals(LocalDate.now()), "default registration date");
        check(ss.getById(s.getId()).getPhone().equals(s.getPhone()), "read subscriber");
        fails(BusinessException.class, () -> ss.create(subscriber(s.getPhone())));
        Subscriber edit = subscriber(s.getPhone()); edit.setId(999L); edit.setRegistrationDate(LocalDate.MIN);
        edit.setEmail("ivan@example.com");
        Subscriber changed = ss.update(s.getId(), edit);
        check(changed.getId().equals(s.getId()) && changed.getRegistrationDate().equals(s.getRegistrationDate()), "preserve subscriber identity/date");
        Subscriber other = subscriber("+79991234568"); other.setEmail("ivan@example.com");
        fails(BusinessException.class, () -> ss.create(other));
        fails(ValidationException.class, () -> ss.getById(null));
        fails(EntityNotFoundException.class, () -> ss.getById(999L));
        fails(EntityNotFoundException.class, () -> ss.update(999L, edit));
        fails(EntityNotFoundException.class, () -> ss.delete(999L));
        fails(ValidationException.class, () -> rs.create(null));
        fails(EntityNotFoundException.class, () -> rs.create(request(999L)));
        fails(ValidationException.class, () -> rs.create(request(null)));
        for (String description : new String[]{null, " ", "1234", " 1234 ", "x".repeat(1001)}) {
            ConnectionRequest bad = request(s.getId()); bad.setDescription(description);
            fails(ValidationException.class, () -> rs.create(bad));
        }
        ConnectionRequest bad = request(s.getId()); bad.setType(null);
        fails(ValidationException.class, () -> rs.create(bad));
        bad.setType(RequestType.NEW_CONNECTION); bad.setStatus(RequestStatus.COMPLETED);
        fails(BusinessException.class, () -> rs.create(bad));
        ConnectionRequest r = rs.create(request(s.getId()));
        check(r.getStatus() == RequestStatus.NEW && r.getCreatedAt() != null
                && r.getCreatedAt().equals(r.getUpdatedAt()), "request defaults");
        ConnectionRequest update = request(s.getId()); update.setStatus(RequestStatus.NEW);
        update.setId(999L); update.setCreatedAt(LocalDateTime.MIN); update.setDescription("12345");
        ConnectionRequest result = rs.update(r.getId(), update);
        check(result.getId().equals(r.getId()) && result.getCreatedAt().equals(r.getCreatedAt()), "preserve request identity/date");
        check(result.getDescription().equals("12345"), "five-character boundary");
        update.setStatus(RequestStatus.COMPLETED);
        fails(BusinessException.class, () -> rs.update(r.getId(), update));
        check(rs.getById(r.getId()).getStatus() == RequestStatus.NEW, "failed update changes nothing");
        fails(ValidationException.class, () -> rs.changeStatus(r.getId(), null));
        fails(EntityNotFoundException.class, () -> rs.delete(999L));
        fails(EntityNotFoundException.class, () -> rs.update(999L, update));
        fails(EntityNotFoundException.class, () -> rs.changeStatus(999L, RequestStatus.NEW));
        fails(ValidationException.class, () -> rs.getById(0L));
        ConnectionRequestQueryService query = new ConnectionRequestQueryService(rs, ss);
        StatisticsService stats = new StatisticsService(ss, rs);
        check(query.searchBySubscriber("Иван").size() == 1, "D search integration");
        check(stats.totalSubscribers() == 1 && stats.activeRequests() == 1, "D statistics integration");
        rs.delete(r.getId()); ss.delete(s.getId());
        check(rs.getAll().isEmpty() && ss.getAll().isEmpty(), "CRUD delete");
    }

    private static void transitions() {
        Set<String> allowed = Set.of("NEW:IN_PROGRESS", "NEW:REJECTED", "NEW:CANCELLED",
                "IN_PROGRESS:APPROVED", "IN_PROGRESS:REJECTED", "IN_PROGRESS:CANCELLED",
                "APPROVED:COMPLETED", "APPROVED:CANCELLED");
        for (RequestStatus from : RequestStatus.values()) {
            for (RequestStatus to : RequestStatus.values()) {
                for (boolean useUpdate : new boolean[]{false, true}) {
                    ServiceChecks f = new ServiceChecks();
                    Subscriber s = f.ss.create(subscriber("+79991234567"));
                    ConnectionRequest r = f.rs.create(request(s.getId()));
                    r.setStatus(from); // Подготовка записи БД с произвольным исходным статусом.
                    ConnectionRequest edit = request(s.getId()); edit.setStatus(to);
                    Runnable action = useUpdate ? () -> f.rs.update(r.getId(), edit)
                            : () -> f.rs.changeStatus(r.getId(), to);
                    if (allowed.contains(from + ":" + to) || (useUpdate && from == to)) {
                        action.run();
                        check(f.rs.getById(r.getId()).getStatus() == to, "allowed transition");
                    } else {
                        fails(BusinessException.class, action);
                        check(f.rs.getById(r.getId()).getStatus() == from, "rejected transition preserved");
                    }
                }
            }
        }
    }

    private static void duplicatesAndDeletion() {
        for (RequestStatus status : RequestStatus.values()) {
            ServiceChecks f = new ServiceChecks();
            Subscriber s = f.ss.create(subscriber("+79991234567"));
            ConnectionRequest r = f.rs.create(request(s.getId())); r.setStatus(status);
            boolean active = Set.of(RequestStatus.NEW, RequestStatus.IN_PROGRESS, RequestStatus.APPROVED).contains(status);
            if (active) {
                fails(BusinessException.class, () -> f.rs.create(request(s.getId())));
                fails(BusinessException.class, () -> f.ss.delete(s.getId()));
                check(f.requests.findAll().size() == 1 && f.ss.getAll().size() == 1, "blocked deletion preserved");
                ConnectionRequest technical = request(s.getId()); technical.setType(RequestType.TECHNICAL_SUPPORT);
                ConnectionRequest second = f.rs.create(technical);
                ConnectionRequest edit = request(s.getId()); edit.setStatus(RequestStatus.NEW);
                fails(BusinessException.class, () -> f.rs.update(second.getId(), edit));
                Subscriber another = f.ss.create(subscriber("+79991234568"));
                ConnectionRequest third = f.rs.create(request(another.getId()));
                fails(BusinessException.class, () -> f.rs.update(third.getId(), edit));
            } else {
                f.rs.create(request(s.getId())); // Терминальная заявка не блокирует новое подключение.
                check(f.requests.findAll().size() == 2, "terminal permits new connection");
                f.requests.findAll().forEach(item -> item.setStatus(status));
                f.ss.delete(s.getId());
                check(f.ss.getAll().isEmpty() && f.rs.getAll().isEmpty(), "delete terminal requests before subscriber FK");
            }
        }
    }

    private static class Memory<T> implements CrudRepository<T, Long> {
        final Map<Long, T> data = new LinkedHashMap<>();
        private final Function<T, Long> id;
        private final BiConsumer<T, Long> setId;
        private long next = 1;
        Memory(Function<T, Long> id, BiConsumer<T, Long> setId) { this.id = id; this.setId = setId; }
        public T save(T entity) { setId.accept(entity, next++); data.put(id.apply(entity), entity); return entity; }
        public Optional<T> findById(Long key) { return Optional.ofNullable(data.get(key)); }
        public List<T> findAll() { return new ArrayList<>(data.values()); }
        public T update(T entity) { data.put(id.apply(entity), entity); return entity; }
        public void deleteById(Long key) { data.remove(key); }
    }
    private class Subscribers extends Memory<Subscriber> implements SubscriberRepository {
        Subscribers() { super(Subscriber::getId, Subscriber::setId); }
        @Override public void deleteById(Long id) {
            if (!requests.findBySubscriberId(id).isEmpty()) throw new AssertionError("FK violation");
            super.deleteById(id);
        }
    }
    private static class Requests extends Memory<ConnectionRequest> implements ConnectionRequestRepository {
        Requests() { super(ConnectionRequest::getId, ConnectionRequest::setId); }
        public List<ConnectionRequest> findBySubscriberId(Long id) {
            return findAll().stream().filter(r -> Objects.equals(r.getSubscriberId(), id)).toList();
        }
    }
}
