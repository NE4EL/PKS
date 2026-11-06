package ru.mirea.isp.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mirea.isp.dto.NoteDtos.NoteRequest;
import ru.mirea.isp.dto.NoteDtos.NoteResponse;
import ru.mirea.isp.service.NoteService;

import java.util.List;
import java.util.UUID;

/** REST-контур операторских заметок. */
@RestController
@RequestMapping("/api/requests/{requestId}/notes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('OPERATOR','ADMIN')")
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) { this.notes = notes; }

    @GetMapping
    public List<NoteResponse> list(@PathVariable UUID requestId) { return notes.list(requestId); }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@PathVariable UUID requestId,
                                               @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(requestId, request));
    }

    @PutMapping("/{noteId}")
    public NoteResponse update(@PathVariable UUID requestId, @PathVariable UUID noteId,
                               @Valid @RequestBody NoteRequest request) {
        return notes.update(requestId, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable UUID requestId, @PathVariable UUID noteId) {
        notes.delete(requestId, noteId);
        return ResponseEntity.noContent().build();
    }
}
