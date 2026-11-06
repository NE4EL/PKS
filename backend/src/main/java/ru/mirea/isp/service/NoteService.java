package ru.mirea.isp.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.isp.domain.Role;
import ru.mirea.isp.domain.ConnectionRequest;
import ru.mirea.isp.domain.ConnectionRequestNote;
import ru.mirea.isp.domain.UserAccount;
import ru.mirea.isp.dto.NoteDtos.NoteRequest;
import ru.mirea.isp.dto.NoteDtos.NoteResponse;
import ru.mirea.isp.error.ConflictException;
import ru.mirea.isp.error.NotFoundException;
import ru.mirea.isp.repository.ConnectionRequestNoteRepository;
import ru.mirea.isp.repository.ConnectionRequestRepository;
import ru.mirea.isp.security.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Управляет заметками и авторскими правами на их изменение. */
@Service
public class NoteService {
    private final ConnectionRequestNoteRepository notes;
    private final ConnectionRequestRepository requests;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public NoteService(ConnectionRequestNoteRepository notes, ConnectionRequestRepository requests,
                       CurrentUserService currentUser, AuditService audit) {
        this.notes = notes;
        this.requests = requests;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID requestId) {
        requireConnectionRequest(requestId);
        return notes.findByRequestIdOrderByPinnedDescCreatedAtDesc(requestId).stream().map(NoteService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID requestId, NoteRequest request) {
        ConnectionRequest ticket = requireConnectionRequest(requestId);
        UserAccount actor = currentUser.requireEntity();
        ConnectionRequestNote note = new ConnectionRequestNote(UUID.randomUUID(), ticket, actor, request.body().trim(), request.pinned());
        note = notes.save(note);
        audit.record("NOTE_CREATED", "REQUEST_NOTE", note.getId(), "Добавлена заметка к заявке", Map.of("requestId", requestId));
        notes.flush();
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID requestId, UUID noteId, NoteRequest request) {
        ConnectionRequestNote note = requireNote(requestId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        if (request.version() == null || !request.version().equals(note.getVersion()))
            throw new ConflictException("Заметка уже изменена. Обновите данные");
        note.setBody(request.body().trim());
        note.setPinned(request.pinned());
        audit.record("NOTE_UPDATED", "REQUEST_NOTE", noteId, "Изменена заметка", Map.of("requestId", requestId));
        notes.flush();
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID requestId, UUID noteId) {
        ConnectionRequestNote note = requireNote(requestId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        notes.delete(note);
        audit.record("NOTE_DELETED", "REQUEST_NOTE", noteId, "Удалена заметка", Map.of("requestId", requestId));
    }

    private ConnectionRequest requireConnectionRequest(UUID id) {
        return requests.findById(id).orElseThrow(() -> new NotFoundException("Заявка не найдена"));
    }

    private ConnectionRequestNote requireNote(UUID requestId, UUID noteId) {
        ConnectionRequestNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        if (!note.getConnectionRequest().getId().equals(requestId)) throw new NotFoundException("Заметка не найдена");
        return note;
    }

    private static void assertOwnerOrAdmin(ConnectionRequestNote note, UserAccount actor) {
        if (!note.getAuthor().getId().equals(actor.getId()) && !actor.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Only note author or administrator can modify it");
        }
    }

    private static NoteResponse toResponse(ConnectionRequestNote note) {
        return new NoteResponse(note.getId(), note.getConnectionRequest().getId(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(), note.isPinned(),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}
