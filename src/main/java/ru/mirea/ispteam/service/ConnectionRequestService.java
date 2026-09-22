package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.repository.ConnectionRequestRepository;
import ru.mirea.ispteam.repository.SubscriberRepository;

import java.util.List;

/*
 * Владелец: B (бизнес-логика). Скелет создан A.
 * Контракт — эталон из PKS.md разд. 4 (участник B). НЕ менять сигнатуры без согласования.
 *
 * TODO(B): реализовать методы с бизнес-правилами из PKS.md разд. 2.4:
 *   - правило 2: заявка только для существующего subscriberId -> EntityNotFoundException
 *   - правило 3: changeStatus проверяет таблицу разрешённых переходов -> BusinessException
 *   - правило 5: не более одной активной заявки NEW_CONNECTION на абонента -> BusinessException
 *   - правило 6: description не пустой и >= 5 символов -> ValidationException
 *
 * Подсказка по переходам статусов (PKS.md разд. 2.3):
 *   NEW -> IN_PROGRESS -> APPROVED -> COMPLETED
 *   NEW, IN_PROGRESS -> REJECTED
 *   NEW, IN_PROGRESS, APPROVED -> CANCELLED
 *   COMPLETED, REJECTED, CANCELLED -> терминальные
 */
public class ConnectionRequestService {

    private final ConnectionRequestRepository requestRepository;
    private final SubscriberRepository subscriberRepository;

    public ConnectionRequestService(ConnectionRequestRepository requestRepository,
                                    SubscriberRepository subscriberRepository) {
        this.requestRepository = requestRepository;
        this.subscriberRepository = subscriberRepository;
    }

    public ConnectionRequest create(ConnectionRequest request) {
        throw new UnsupportedOperationException("TODO(B): реализовать ConnectionRequestService.create");
    }

    public ConnectionRequest getById(Long id) {
        throw new UnsupportedOperationException("TODO(B): реализовать ConnectionRequestService.getById");
    }

    public List<ConnectionRequest> getAll() {
        throw new UnsupportedOperationException("TODO(B): реализовать ConnectionRequestService.getAll");
    }

    public ConnectionRequest update(Long id, ConnectionRequest updated) {
        throw new UnsupportedOperationException("TODO(B): реализовать ConnectionRequestService.update");
    }

    // Проверяет таблицу разрешённых переходов (PKS.md разд. 2.3).
    public void changeStatus(Long id, RequestStatus newStatus) {
        throw new UnsupportedOperationException("TODO(B): реализовать ConnectionRequestService.changeStatus");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("TODO(B): реализовать ConnectionRequestService.delete");
    }
}
