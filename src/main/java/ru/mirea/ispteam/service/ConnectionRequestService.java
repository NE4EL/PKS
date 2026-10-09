package ru.mirea.ispteam.service;

import ru.mirea.ispteam.exception.BusinessException;
import ru.mirea.ispteam.exception.EntityNotFoundException;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.repository.ConnectionRequestRepository;
import ru.mirea.ispteam.repository.SubscriberRepository;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Владелец: B. CRUD заявок и переходы статусов из PKS.md. */
public class ConnectionRequestService {
    private final ConnectionRequestRepository requestRepository;
    private final SubscriberRepository subscriberRepository;

    public ConnectionRequestService(ConnectionRequestRepository requestRepository,
                                    SubscriberRepository subscriberRepository) {
        this.requestRepository = requestRepository;
        this.subscriberRepository = subscriberRepository;
    }

    public ConnectionRequest create(ConnectionRequest request) {
        ConnectionRequest validated = validate(request);
        if (request.getStatus() != null && request.getStatus() != RequestStatus.NEW) {
            throw new BusinessException("Новая заявка должна иметь статус «Новая»");
        }
        validated.setStatus(RequestStatus.NEW);
        checkActiveConnection(validated, null);
        LocalDateTime now = LocalDateTime.now();
        validated.setCreatedAt(now);
        validated.setUpdatedAt(now);
        return requestRepository.save(validated);
    }

    public ConnectionRequest getById(Long id) {
        validateId(id, "заявки");
        return requestRepository.findById(id).orElseThrow(() ->
                new EntityNotFoundException("Заявка с id=" + id + " не найдена"));
    }

    public List<ConnectionRequest> getAll() {
        return requestRepository.findAll();
    }

    public ConnectionRequest update(Long id, ConnectionRequest updated) {
        ConnectionRequest current = getById(id);
        ConnectionRequest validated = validate(updated);
        if (updated.getStatus() == null) {
            throw new ValidationException("status", "Статус заявки обязателен");
        }
        // Редактирование с прежним статусом допустимо; смена подчиняется общей таблице.
        if (current.getStatus() != updated.getStatus()) {
            checkTransition(current.getStatus(), updated.getStatus());
        }
        validated.setId(id);
        validated.setStatus(updated.getStatus());
        checkActiveConnection(validated, id);
        validated.setCreatedAt(current.getCreatedAt());
        validated.setUpdatedAt(LocalDateTime.now());
        return requestRepository.update(validated);
    }

    public void changeStatus(Long id, RequestStatus newStatus) {
        ConnectionRequest current = getById(id);
        checkTransition(current.getStatus(), newStatus);
        // Копия не меняет исходный объект до успешной проверки и сохранения.
        ConnectionRequest changed = new ConnectionRequest(id, current.getSubscriberId(),
                current.getType(), newStatus, current.getTariffPlan(), current.getDescription(),
                current.getCreatedAt(), LocalDateTime.now());
        checkActiveConnection(changed, id);
        requestRepository.update(changed);
    }

    public void delete(Long id) {
        getById(id);
        requestRepository.deleteById(id);
    }

    /**
     * КР2: статусы, в которые можно перейти из {@code from} (таблица из PKS.md разд. 2.3).
     * Используется и для проверки, и в JavaFX-форме, чтобы предлагать только допустимые варианты.
     */
    public static Set<RequestStatus> allowedTransitions(RequestStatus from) {
        if (from == null) return EnumSet.noneOf(RequestStatus.class);
        return switch (from) {
            case NEW -> EnumSet.of(RequestStatus.IN_PROGRESS, RequestStatus.REJECTED, RequestStatus.CANCELLED);
            case IN_PROGRESS -> EnumSet.of(RequestStatus.APPROVED, RequestStatus.REJECTED, RequestStatus.CANCELLED);
            case APPROVED -> EnumSet.of(RequestStatus.COMPLETED, RequestStatus.CANCELLED);
            case COMPLETED, REJECTED, CANCELLED -> EnumSet.noneOf(RequestStatus.class);
        };
    }

    private ConnectionRequest validate(ConnectionRequest request) {
        if (request == null) throw new ValidationException("Данные заявки обязательны");
        if (request.getSubscriberId() == null || request.getSubscriberId() <= 0) {
            throw new ValidationException("subscriber", "Выберите абонента");
        }
        if (request.getType() == null) throw new ValidationException("type", "Тип заявки обязателен");
        String description = request.getDescription();
        if (description == null || description.strip().length() < 5
                || description.strip().length() > 1000) {
            throw new ValidationException("description", "Описание заявки должно содержать от 5 до 1000 символов");
        }
        String tariff = request.getTariffPlan() == null ? null : request.getTariffPlan().strip();
        if (tariff != null && tariff.length() > 100) {
            throw new ValidationException("tariffPlan", "Тарифный план не должен превышать 100 символов");
        }
        if (tariff != null && tariff.isEmpty()) tariff = null;
        subscriberRepository.findById(request.getSubscriberId()).orElseThrow(() ->
                new EntityNotFoundException("Абонент с id=" + request.getSubscriberId() + " не найден"));
        return new ConnectionRequest(request.getSubscriberId(), request.getType(), request.getStatus(),
                tariff, description.strip(), null, null);
    }

    private void checkActiveConnection(ConnectionRequest request, Long excludedId) {
        if (request.getType() != RequestType.NEW_CONNECTION || !request.getStatus().isActive()) return;
        boolean duplicate = requestRepository.findBySubscriberId(request.getSubscriberId()).stream()
                .anyMatch(existing -> !Objects.equals(existing.getId(), excludedId)
                        && existing.getType() == RequestType.NEW_CONNECTION && existing.getStatus().isActive());
        if (duplicate) throw new BusinessException("У абонента уже есть активная заявка на подключение");
    }

    private static void checkTransition(RequestStatus from, RequestStatus to) {
        if (to == null) throw new ValidationException("status", "Новый статус обязателен");
        if (!allowedTransitions(from).contains(to)) {
            throw new BusinessException("Недопустимый переход статуса: «"
                    + (from == null ? "—" : from.getTitle()) + "» -> «" + to.getTitle() + "»");
        }
    }

    private static void validateId(Long id, String entity) {
        if (id == null || id <= 0) {
            throw new ValidationException("ID " + entity + " должен быть положительным числом");
        }
    }
}
