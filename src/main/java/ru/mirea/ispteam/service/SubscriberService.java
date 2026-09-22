package ru.mirea.ispteam.service;

import ru.mirea.ispteam.model.Subscriber;
import ru.mirea.ispteam.repository.ConnectionRequestRepository;
import ru.mirea.ispteam.repository.SubscriberRepository;

import java.util.List;

/*
 * Владелец: B (бизнес-логика). Скелет создан A.
 *
 * TODO(B): реализовать CRUD с бизнес-правилами из PKS.md разд. 2.4:
 *   - правило 1: валидация fullName/phone (формат +7XXXXXXXXXX) -> ValidationException
 *   - правило 4: запрет удаления абонента при активных заявках (NEW/IN_PROGRESS/APPROVED) -> BusinessException
 *
 * Зависимости уже проброшены в конструктор (репозитории слоя A). Сигнатуры публичных
 * методов согласуйте с командой, если будете менять.
 */
public class SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final ConnectionRequestRepository requestRepository;

    public SubscriberService(SubscriberRepository subscriberRepository,
                             ConnectionRequestRepository requestRepository) {
        this.subscriberRepository = subscriberRepository;
        this.requestRepository = requestRepository;
    }

    public Subscriber create(Subscriber subscriber) {
        throw new UnsupportedOperationException("TODO(B): реализовать SubscriberService.create");
    }

    public Subscriber getById(Long id) {
        throw new UnsupportedOperationException("TODO(B): реализовать SubscriberService.getById");
    }

    public List<Subscriber> getAll() {
        throw new UnsupportedOperationException("TODO(B): реализовать SubscriberService.getAll");
    }

    public Subscriber update(Long id, Subscriber updated) {
        throw new UnsupportedOperationException("TODO(B): реализовать SubscriberService.update");
    }

    public void delete(Long id) {
        throw new UnsupportedOperationException("TODO(B): реализовать SubscriberService.delete");
    }
}
