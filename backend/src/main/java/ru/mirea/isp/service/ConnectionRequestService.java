package ru.mirea.isp.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.isp.config.AppProperties;
import ru.mirea.isp.domain.ConnectionRequest;
import ru.mirea.isp.domain.ConnectionRequestStatus;
import ru.mirea.isp.domain.Subscriber;
import ru.mirea.isp.domain.UserAccount;
import ru.mirea.isp.dto.ConnectionRequestDtos.BulkAction;
import ru.mirea.isp.dto.ConnectionRequestDtos.BulkFailure;
import ru.mirea.isp.dto.ConnectionRequestDtos.BulkRequest;
import ru.mirea.isp.dto.ConnectionRequestDtos.BulkResult;
import ru.mirea.isp.dto.ConnectionRequestDtos.ConnectionRequestRequest;
import ru.mirea.isp.dto.ConnectionRequestDtos.ConnectionRequestResponse;
import ru.mirea.isp.error.BadRequestException;
import ru.mirea.isp.error.ConflictException;
import ru.mirea.isp.error.NotFoundException;
import ru.mirea.isp.filter.SmartFilterParser;
import ru.mirea.isp.filter.ConnectionRequestFilter;
import ru.mirea.isp.filter.ConnectionRequestSpecifications;
import ru.mirea.isp.repository.ConnectionRequestNoteRepository;
import ru.mirea.isp.repository.ConnectionRequestRepository;
import ru.mirea.isp.repository.SubscriberRepository;
import ru.mirea.isp.security.CurrentUserService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Основной прикладной сервис карточек заявок. */
@Service
public class ConnectionRequestService {
    private final ConnectionRequestRepository requests;
    private final ConnectionRequestNoteRepository notes;
    private final SubscriberRepository subscribers;
    private final CurrentUserService currentUser;
    private final SmartFilterParser parser;
    private final ConnectionRequestMapper mapper;
    private final PriorityCalculator priorityCalculator;
    private final org.springframework.transaction.support.TransactionTemplate rowTransaction;
    private final AuditService audit;
    private final AppProperties properties;

    public ConnectionRequestService(ConnectionRequestRepository requests, ConnectionRequestNoteRepository notes,
                          SubscriberRepository subscribers, CurrentUserService currentUser,
                          SmartFilterParser parser, ConnectionRequestMapper mapper,
                          PriorityCalculator priorityCalculator, AuditService audit,
                          AppProperties properties, org.springframework.transaction.PlatformTransactionManager transactionManager) {
        this.requests = requests;
        this.notes = notes;
        this.subscribers = subscribers;
        this.currentUser = currentUser;
        this.parser = parser;
        this.mapper = mapper;
        this.priorityCalculator = priorityCalculator;
        this.audit = audit;
        this.rowTransaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
        this.rowTransaction.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.properties = properties;
    }

    /** Выполняет безопасный умный поиск с пагинацией. */
    @Transactional(readOnly = true)
    public Page<ConnectionRequestResponse> search(String query, Pageable pageable) {
        ConnectionRequestFilter filter = parser.parse(query);
        return requests.findAll(ConnectionRequestSpecifications.from(filter), pageable).map(mapper::toResponse);
    }

    /** Возвращает одну карточку независимо от архивного статуса. */
    @Transactional(readOnly = true)
    public ConnectionRequestResponse get(UUID id) {
        return mapper.toResponse(require(id));
    }

    /** Создаёт карточку и рассчитывает приоритет на сервере. */
    @Transactional
    public ConnectionRequestResponse create(ConnectionRequestRequest request) {
        UserAccount actor = currentUser.requireEntity();
        validateExternalId(request.externalId(), null);
        ConnectionRequest ticket = new ConnectionRequest(UUID.randomUUID(), normalizeRequired(request.title()),
                normalizeRequired(request.description()), actor);
        apply(ticket, request, actor);
        ticket = requests.save(ticket);
        audit.record("REQUEST_CREATED", "REQUEST", ticket.getId(),
                "Создана карточка заявки " + displayName(ticket), Map.of());
        requests.flush();
        return mapper.toResponse(ticket);
    }

    /** Изменяет карточку только при совпадении optimistic version. */
    @Transactional
    public ConnectionRequestResponse update(UUID id, ConnectionRequestRequest request) {
        ConnectionRequest ticket = require(id);
        verifyVersion(request.version(), ticket.getVersion());
        validateExternalId(request.externalId(), id);
        UserAccount actor = currentUser.requireEntity();
        apply(ticket, request, actor);
        audit.record("REQUEST_UPDATED", "REQUEST", id,
                "Изменена карточка заявки " + displayName(ticket), Map.of("version", ticket.getVersion()));
        requests.flush();
        return mapper.toResponse(ticket);
    }

    /** Архивирует карточку вместо физического удаления. */
    @Transactional
    public ConnectionRequestResponse archive(UUID id) {
        ConnectionRequest ticket = require(id);
        archiveInternal(ticket, currentUser.requireEntity());
        audit.record("REQUEST_ARCHIVED", "REQUEST", id,
                "Карточка заявки перемещена в архив", Map.of());
        requests.flush();
        return mapper.toResponse(ticket);
    }

    /** Возвращает карточку из архива. */
    @Transactional
    public ConnectionRequestResponse restore(UUID id) {
        ConnectionRequest ticket = require(id);
        ticket.setArchived(false);
        ticket.setArchivedAt(null);
        ticket.setArchivedBy(null);
        ticket.setUpdatedBy(currentUser.requireEntity());
        audit.record("REQUEST_RESTORED", "REQUEST", id,
                "Карточка заявки восстановлена из архива", Map.of());
        requests.flush();
        return mapper.toResponse(ticket);
    }

    /**
     * Безвозвратно удаляет запись только при включённом feature flag и явном подтверждении.
     */
    @Transactional
    public void permanentDelete(UUID id, String confirmation) {
        if (!properties.permanentDeleteEnabled()) {
            throw new BadRequestException("Безвозвратное удаление отключено конфигурацией сервера");
        }
        if (!"PERMANENTLY_DELETE".equals(confirmation)) {
            throw new BadRequestException("Для удаления требуется заголовок X-Confirm-Delete: PERMANENTLY_DELETE");
        }
        ConnectionRequest ticket = require(id);
        String name = displayName(ticket);
        requests.delete(ticket);
        audit.record("REQUEST_PERMANENTLY_DELETED", "REQUEST", id,
                "Безвозвратно удалена карточка " + name, Map.of());
    }

    /** Выполняет ограниченную массовую операцию и сообщает ошибки отдельных UUID. */
    @Transactional
    public BulkResult bulk(BulkRequest request) {
        validateBulkRequest(request);
        UserAccount actor = currentUser.requireEntity();
        int succeeded = 0;
        var failures = new ArrayList<BulkFailure>();
        for (UUID id : request.ids()) {
            try {
                rowTransaction.executeWithoutResult(tx -> {
                    ConnectionRequest ticket = require(id);
                    applyBulk(ticket, request, actor);
                    requests.flush();
                    audit.record("REQUEST_BULK_" + request.action().name(), "REQUEST", id, "Массовое изменение заявки", Map.of());
                });
                succeeded++;
            } catch (RuntimeException ex) {
                failures.add(new BulkFailure(id, ex instanceof NotFoundException ? "Заявка не найдена" : "Не удалось изменить заявку"));
            }
        }
        audit.record("REQUEST_BULK_" + request.action().name(), "REQUEST", null,
                "Выполнена массовая операция", Map.of("requested", request.ids().size(), "succeeded", succeeded));
        return new BulkResult(request.ids().size(), succeeded, failures);
    }

    /** Используется CSV-сервисом внутри транзакции без дублирования правил. */
    ConnectionRequest upsertFromImport(ConnectionRequest existing, ConnectionRequestRequest request, UserAccount actor) {
        if (existing == null) {
            ConnectionRequest created = new ConnectionRequest(UUID.randomUUID(), normalizeRequired(request.title()),
                    normalizeRequired(request.description()), actor);
            apply(created, request, actor);
            return requests.save(created);
        }
        apply(existing, request, actor);
        return existing;
    }

    private void apply(ConnectionRequest ticket, ConnectionRequestRequest request, UserAccount actor) {
        ticket.setExternalId(normalizeNullable(request.externalId()));
        ticket.setTitle(normalizeRequired(request.title()));
        ticket.setDescription(normalizeRequired(request.description()));
        ticket.setAddress(normalizeNullable(request.address()));
        ticket.setEmail(normalizeEmail(request.email()));
        ticket.setPhone(normalizeNullable(request.phone()));
        ticket.setDueDate(request.dueDate());
        ticket.setSubscriber(resolveSubscriber(request.subscriberId()));
        ticket.setStatus(request.status());
        ticket.setType(request.type());
        ticket.setTariffPlan(normalizeNullable(request.tariffPlan()));
        ticket.setEstimatedHours(request.estimatedHours());
        ticket.setUrgent(request.urgent());
        ticket.setTags(normalizeTags(request.tags()));
        ticket.setPriorityLevel(priorityCalculator.calculate(request.status(), request.estimatedHours(), request.urgent()));
        ticket.setUpdatedBy(actor);
    }

    private void applyBulk(ConnectionRequest ticket, BulkRequest request, UserAccount actor) {
        switch (request.action()) {
            case ARCHIVE -> archiveInternal(ticket, actor);
            case RESTORE -> {
                ticket.setArchived(false);
                ticket.setArchivedAt(null);
                ticket.setArchivedBy(null);
                ticket.setUpdatedBy(actor);
            }
            case SET_STATUS -> {
                ticket.setStatus(request.status());
                ticket.setPriorityLevel(priorityCalculator.calculate(ticket.getStatus(), ticket.getEstimatedHours(), ticket.isUrgent()));
                ticket.setUpdatedBy(actor);
            }
            case ADD_TAG -> {
                Set<String> tags = new LinkedHashSet<>(ticket.getTags());
                tags.add(normalizeTag(request.tag()));
                if (tags.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
                ticket.setTags(tags);
                ticket.setUpdatedBy(actor);
            }
            case REMOVE_TAG -> {
                Set<String> tags = new LinkedHashSet<>(ticket.getTags());
                tags.remove(normalizeTag(request.tag()));
                ticket.setTags(tags);
                ticket.setUpdatedBy(actor);
            }
        }
    }

    private static void validateBulkRequest(BulkRequest request) {
        if (request.action() == BulkAction.SET_STATUS && request.status() == null) {
            throw new BadRequestException("Для SET_STATUS требуется status");
        }
        if ((request.action() == BulkAction.ADD_TAG || request.action() == BulkAction.REMOVE_TAG)
                && (request.tag() == null || request.tag().isBlank())) {
            throw new BadRequestException("Для операции с тегом требуется tag");
        }
    }

    private static void archiveInternal(ConnectionRequest ticket, UserAccount actor) {
        ticket.setArchived(true);
        ticket.setArchivedAt(Instant.now());
        ticket.setArchivedBy(actor);
        ticket.setUpdatedBy(actor);
    }

    private ConnectionRequest require(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private Subscriber resolveSubscriber(UUID id) {
        if (id == null) throw new BadRequestException("Абонент обязателен");
        Subscriber subscriber = subscribers.findById(id).orElseThrow(() -> new BadRequestException("Абонент не найден"));
        if (!subscriber.isActive()) throw new BadRequestException("Нельзя назначить неактивную абонента");
        return subscriber;
    }

    private void validateExternalId(String externalId, UUID currentId) {
        String normalized = normalizeNullable(externalId);
        if (normalized == null) return;
        boolean exists = currentId == null
                ? requests.existsByExternalIdIgnoreCase(normalized)
                : requests.existsByExternalIdIgnoreCaseAndIdNot(normalized, currentId);
        if (exists) throw new ConflictException("Внешний идентификатор уже используется");
    }

    private static void verifyVersion(Long requested, long actual) {
        if (requested == null) throw new BadRequestException("Для изменения требуется поле version");
        if (requested != actual) throw new ConflictException("Карточка уже изменена. Обновите данные и повторите операцию");
    }

    private static Set<String> normalizeTags(Set<String> tags) {
        if (tags == null) return Set.of();
        LinkedHashSet<String> result = new LinkedHashSet<>();
        for (String tag : tags) result.add(normalizeTag(tag));
        if (result.size() > 20) throw new BadRequestException("Допустимо не более 20 тегов");
        return result;
    }

    private static String normalizeTag(String value) {
        String normalized = normalizeRequired(value).toLowerCase(Locale.ROOT);
        if (normalized.length() > 50) throw new BadRequestException("Тег длиннее 50 символов");
        return normalized;
    }

    private static String normalizeEmail(String value) {
        String normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String normalizeRequired(String value) {
        if (value == null || value.trim().isEmpty()) throw new BadRequestException("Обязательное текстовое поле не заполнено");
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim().replaceAll("\\s+", " ");
    }

    private static String displayName(ConnectionRequest ticket) {
        return ticket.getDescription() + " " + ticket.getTitle();
    }
}
