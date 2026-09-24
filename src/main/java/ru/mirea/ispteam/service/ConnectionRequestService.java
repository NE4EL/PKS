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
import java.util.List;
import java.util.Objects;

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
            throw new BusinessException("Новая заявка должна иметь статус NEW");
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
            throw new ValidationException("Статус заявки обязателен");
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

    private ConnectionRequest validate(ConnectionRequest request) {
        if (request == null) throw new ValidationException("Данные заявки обязательны");
        validateId(request.getSubscriberId(), "абонента");
        if (request.getType() == null) throw new ValidationException("Тип заявки обязателен");
        String description = request.getDescription();
        if (description == null || description.strip().length() < 5
                || description.strip().length() > 1000) {
            throw new ValidationException("Описание заявки должно содержать от 5 до 1000 символов");
        }
        String tariff = request.getTariffPlan() == null ? null : request.getTariffPlan().strip();
        if (tariff != null && tariff.length() > 100) {
            throw new ValidationException("Тарифный план не должен превышать 100 символов");
        }
        if (tariff != null && tariff.isEmpty()) tariff = null;
        subscriberRepository.findById(request.getSubscriberId()).orElseThrow(() ->
                new EntityNotFoundException("Абонент с id=" + request.getSubscriberId() + " не найден"));
        return new ConnectionRequest(request.getSubscriberId(), request.getType(), request.getStatus(),
                tariff, description.strip(), null, null);
    }

    private void checkActiveConnection(ConnectionRequest request, Long excludedId) {
        if (request.getType() != RequestType.NEW_CONNECTION || !isActive(request.getStatus())) return;
        boolean duplicate = requestRepository.findBySubscriberId(request.getSubscriberId()).stream()
                .anyMatch(existing -> !Objects.equals(existing.getId(), excludedId)
                        && existing.getType() == RequestType.NEW_CONNECTION && isActive(existing.getStatus()));
        if (duplicate) throw new BusinessException("У абонента уже есть активная заявка на подключение");
    }

    private static void checkTransition(RequestStatus from, RequestStatus to) {
        if (to == null) throw new ValidationException("Новый статус обязателен");
        boolean allowed = from != null && switch (from) {
            case NEW -> to == RequestStatus.IN_PROGRESS || to == RequestStatus.REJECTED
                    || to == RequestStatus.CANCELLED;
            case IN_PROGRESS -> to == RequestStatus.APPROVED || to == RequestStatus.REJECTED
                    || to == RequestStatus.CANCELLED;
            case APPROVED -> to == RequestStatus.COMPLETED || to == RequestStatus.CANCELLED;
            case COMPLETED, REJECTED, CANCELLED -> false;
        };
        if (!allowed) throw new BusinessException("Недопустимый переход статуса: " + from + " -> " + to);
    }

    private static void validateId(Long id, String entity) {
        if (id == null || id <= 0) {
            throw new ValidationException("ID " + entity + " должен быть положительным числом");
        }
    }

    private static boolean isActive(RequestStatus status) {
        return status == RequestStatus.NEW || status == RequestStatus.IN_PROGRESS
                || status == RequestStatus.APPROVED;
    }
}
