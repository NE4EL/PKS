package ru.mirea.studenthub.controller;

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
import ru.mirea.studenthub.dto.NoteDtos.NoteRequest;
import ru.mirea.studenthub.dto.NoteDtos.NoteResponse;
import ru.mirea.studenthub.service.NoteService;

import java.util.List;
import java.util.UUID;

/** REST-контур преподавательских заметок. */
@RestController
@RequestMapping("/api/students/{studentId}/notes")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasAnyRole('TEACHER','ADMIN')")
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) { this.notes = notes; }

    @GetMapping
    public List<NoteResponse> list(@PathVariable UUID studentId) { return notes.list(studentId); }

    @PostMapping
    public ResponseEntity<NoteResponse> create(@PathVariable UUID studentId,
                                               @Valid @RequestBody NoteRequest request) {
        return ResponseEntity.status(201).body(notes.create(studentId, request));
    }

    @PutMapping("/{noteId}")
    public NoteResponse update(@PathVariable UUID studentId, @PathVariable UUID noteId,
                               @Valid @RequestBody NoteRequest request) {
        return notes.update(studentId, noteId, request);
    }

    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(@PathVariable UUID studentId, @PathVariable UUID noteId) {
        notes.delete(studentId, noteId);
        return ResponseEntity.noContent().build();
    }
}
