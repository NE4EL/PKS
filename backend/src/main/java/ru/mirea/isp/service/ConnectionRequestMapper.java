package ru.mirea.isp.service;

import org.springframework.stereotype.Component;
import ru.mirea.isp.domain.ConnectionRequest;
import ru.mirea.isp.dto.ConnectionRequestDtos.ConnectionRequestResponse;

import java.util.Set;

/** Преобразует entity в публичный DTO внутри активной транзакции. */
@Component
public class ConnectionRequestMapper {
    public ConnectionRequestResponse toResponse(ConnectionRequest request) {
        return new ConnectionRequestResponse(request.getId(), request.getExternalId(), request.getTitle(),
                request.getDescription(), request.getAddress(), request.getEmail(),
                request.getPhone(), request.getDueDate(),
                request.getSubscriber() == null ? null : request.getSubscriber().getId(),
                request.getSubscriber() == null ? null : request.getSubscriber().getCode(),
                request.getStatus(), request.getType(), request.getTariffPlan(), request.getEstimatedHours(), request.isUrgent(), request.getPriorityLevel(),
                request.isArchived(), request.getArchivedAt(), Set.copyOf(request.getTags()),
                request.getCreatedAt(), request.getUpdatedAt(), request.getVersion());
    }
}
