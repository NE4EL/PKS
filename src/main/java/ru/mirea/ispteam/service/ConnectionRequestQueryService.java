package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;

import java.util.List;

/*
 * Владелец: D (поиск/фильтрация/сортировка). Скелет создан A.
 * Контракт — эталон из PKS.md разд. 4 (участник D). НЕ менять сигнатуры без согласования.
 *
 * TODO(D): реализовать через Stream API поверх ConnectionRequestService.getAll()
 * (не дублировать бизнес-логику B, не обращаться к репозиториям напрямую).
 *   - searchBySubscriber: по ФИО/телефону абонента (подстрока)
 *   - searchByDescription: по ключевому слову в description
 *   - filterByStatus / filterByType
 *   - sortByCreatedAt(asc) / sortByStatus
 */
public class ConnectionRequestQueryService {

    private final ConnectionRequestService requestService;

    public ConnectionRequestQueryService(ConnectionRequestService requestService) {
        this.requestService = requestService;
    }

    public List<ConnectionRequest> searchBySubscriber(String query) {
        throw new UnsupportedOperationException("TODO(D): реализовать searchBySubscriber");
    }

    public List<ConnectionRequest> searchByDescription(String keyword) {
        throw new UnsupportedOperationException("TODO(D): реализовать searchByDescription");
    }

    public List<ConnectionRequest> filterByStatus(RequestStatus status) {
        throw new UnsupportedOperationException("TODO(D): реализовать filterByStatus");
    }

    public List<ConnectionRequest> filterByType(RequestType type) {
        throw new UnsupportedOperationException("TODO(D): реализовать filterByType");
    }

    public List<ConnectionRequest> sortByCreatedAt(boolean ascending) {
        throw new UnsupportedOperationException("TODO(D): реализовать sortByCreatedAt");
    }

    public List<ConnectionRequest> sortByStatus() {
        throw new UnsupportedOperationException("TODO(D): реализовать sortByStatus");
    }
}
