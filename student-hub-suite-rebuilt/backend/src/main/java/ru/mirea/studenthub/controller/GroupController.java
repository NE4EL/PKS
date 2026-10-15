package ru.mirea.studenthub.controller;

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
import ru.mirea.studenthub.dto.GroupDtos.GroupRequest;
import ru.mirea.studenthub.dto.GroupDtos.GroupResponse;
import ru.mirea.studenthub.service.GroupService;

import java.util.List;
import java.util.UUID;

/** REST-контур учебных групп. */
@RestController
@RequestMapping("/api/groups")
@SecurityRequirement(name = "bearerAuth")
public class GroupController {
    private final GroupService groups;

    public GroupController(GroupService groups) { this.groups = groups; }

    @GetMapping
    @PreAuthorize("hasAnyRole('VIEWER','TEACHER','ADMIN')")
    public List<GroupResponse> list(@RequestParam(defaultValue = "false") boolean activeOnly) {
        return groups.list(activeOnly);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroupResponse> create(@Valid @RequestBody GroupRequest request) {
        return ResponseEntity.status(201).body(groups.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public GroupResponse update(@PathVariable UUID id, @Valid @RequestBody GroupRequest request) {
        return groups.update(id, request);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public GroupResponse setActive(@PathVariable UUID id, @RequestParam boolean active, @RequestParam long version) {
        return groups.setActive(id, active, version);
    }
}
