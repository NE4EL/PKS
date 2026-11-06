package ru.mirea.isp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.isp.domain.Subscriber;
import ru.mirea.isp.dto.SubscriberDtos.SubscriberRequest;
import ru.mirea.isp.dto.SubscriberDtos.SubscriberResponse;
import ru.mirea.isp.error.BadRequestException;
import ru.mirea.isp.error.ConflictException;
import ru.mirea.isp.error.NotFoundException;
import ru.mirea.isp.repository.SubscriberRepository;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/** Прикладные операции над абонентами. */
@Service
public class SubscriberService {
    private final SubscriberRepository subscribers;
    private final AuditService audit;

    public SubscriberService(SubscriberRepository subscribers, AuditService audit) {
        this.subscribers = subscribers;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<SubscriberResponse> list(boolean activeOnly) {
        return (activeOnly ? subscribers.findByActiveTrueOrderByCodeAsc() : subscribers.findAllByOrderByCodeAsc())
                .stream().map(SubscriberService::toResponse).toList();
    }

    @Transactional
    public SubscriberResponse create(SubscriberRequest request) {
        String code = normalizeCode(request.code());
        if (subscribers.existsByCodeIgnoreCase(code)) throw new ConflictException("Абонент с таким кодом уже существует");
        Subscriber subscriber = new Subscriber(UUID.randomUUID(), code, normalizeNullable(request.name()), request.registrationYear());
        if (request.active() != null) subscriber.setActive(request.active());
        subscriber.setPhone(request.phone());
        subscriber.setEmail(request.email());
        subscriber.setAddress(request.address());
        subscriber = subscribers.save(subscriber);
        audit.record("SUBSCRIBER_CREATED", "SUBSCRIBER", subscriber.getId(), "Создан абонент " + code, Map.of());
        subscribers.flush();
        return toResponse(subscriber);
    }

    @Transactional
    public SubscriberResponse update(UUID id, SubscriberRequest request) {
        Subscriber subscriber = require(id);
        if (request.version() == null || !request.version().equals(subscriber.getVersion()))
            throw new ConflictException("Абонент уже изменён. Обновите список");
        String code = normalizeCode(request.code());
        subscribers.findByCodeIgnoreCase(code).filter(other -> !other.getId().equals(id)).ifPresent(other -> {
            throw new ConflictException("Абонент с таким кодом уже существует");
        });
        subscriber.setCode(code);
        subscriber.setName(normalizeNullable(request.name()));
        subscriber.setRegistrationYear(request.registrationYear());
        subscriber.setPhone(request.phone());
        subscriber.setEmail(request.email());
        subscriber.setAddress(request.address());
        if (request.active() != null) subscriber.setActive(request.active());
        audit.record("SUBSCRIBER_UPDATED", "SUBSCRIBER", id, "Изменён абонент " + code, Map.of());
        subscribers.flush();
        return toResponse(subscriber);
    }

    @Transactional
    public SubscriberResponse setActive(UUID id, boolean active, long version) {
        Subscriber subscriber = require(id);
        if (version != subscriber.getVersion()) throw new ConflictException("Абонент уже изменён. Обновите список");
        subscriber.setActive(active);
        audit.record(active ? "SUBSCRIBER_ACTIVATED" : "SUBSCRIBER_DEACTIVATED", "SUBSCRIBER", id,
                (active ? "Активирована" : "Деактивирована") + " абонент " + subscriber.getCode(), Map.of());
        subscribers.flush();
        return toResponse(subscriber);
    }

    private Subscriber require(UUID id) {
        return subscribers.findById(id).orElseThrow(() -> new NotFoundException("Абонент не найден"));
    }

    private static SubscriberResponse toResponse(Subscriber subscriber) {
        return new SubscriberResponse(subscriber.getId(), subscriber.getCode(), subscriber.getName(), subscriber.getRegistrationYear(),
                subscriber.getPhone(), subscriber.getEmail(), subscriber.getAddress(), subscriber.isActive(), subscriber.getCreatedAt(), subscriber.getUpdatedAt(), subscriber.getVersion());
    }

    private static String normalizeCode(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("Код абонента обязателен");
        return value.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim().replaceAll("\\s+", " ");
    }
}
