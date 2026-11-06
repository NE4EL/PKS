package ru.mirea.isp.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.isp.dto.SubscriberDtos.SubscriberRequest;
import ru.mirea.isp.dto.SubscriberDtos.SubscriberResponse;
import ru.mirea.isp.service.SubscriberService;

import java.util.List;
import java.util.UUID;

/** REST-контур абонентов. */
@RestController
@RequestMapping("/api/subscribers")
@SecurityRequirement(name = "bearerAuth")
public class SubscriberController {
    private final SubscriberService subscribers;

    public SubscriberController(SubscriberService subscribers) { this.subscribers = subscribers; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','OPERATOR','ADMIN')")
    public List<SubscriberResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return subscribers.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SubscriberResponse> create(@Valid @RequestBody SubscriberRequest request) {
        return ResponseEntity.status(201).body(subscribers.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriberResponse update(@PathVariable UUID id, @Valid @RequestBody SubscriberRequest request) {
        return subscribers.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public SubscriberResponse setActive(@PathVariable UUID id, @RequestParam boolean active, @RequestParam long version) {
        return subscribers.setActive(id, active, version);
    }
}
