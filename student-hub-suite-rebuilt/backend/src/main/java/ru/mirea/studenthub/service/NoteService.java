package ru.mirea.studenthub.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.studenthub.domain.Role;
import ru.mirea.studenthub.domain.Student;
import ru.mirea.studenthub.domain.StudentNote;
import ru.mirea.studenthub.domain.UserAccount;
import ru.mirea.studenthub.dto.NoteDtos.NoteRequest;
import ru.mirea.studenthub.dto.NoteDtos.NoteResponse;
import ru.mirea.studenthub.error.ConflictException;
import ru.mirea.studenthub.error.NotFoundException;
import ru.mirea.studenthub.repository.StudentNoteRepository;
import ru.mirea.studenthub.repository.StudentRepository;
import ru.mirea.studenthub.security.CurrentUserService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Управляет заметками и авторскими правами на их изменение. */
@Service
public class NoteService {
    private final StudentNoteRepository notes;
    private final StudentRepository students;
    private final CurrentUserService currentUser;
    private final AuditService audit;

    public NoteService(StudentNoteRepository notes, StudentRepository students,
                       CurrentUserService currentUser, AuditService audit) {
        this.notes = notes;
        this.students = students;
        this.currentUser = currentUser;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteResponse> list(UUID studentId) {
        requireStudent(studentId);
        return notes.findByStudentIdOrderByPinnedDescCreatedAtDesc(studentId).stream().map(NoteService::toResponse).toList();
    }

    @Transactional
    public NoteResponse create(UUID studentId, NoteRequest request) {
        Student student = requireStudent(studentId);
        UserAccount actor = currentUser.requireEntity();
        StudentNote note = new StudentNote(UUID.randomUUID(), student, actor, request.body().trim(), request.pinned());
        notes.save(note);
        audit.record("NOTE_CREATED", "STUDENT_NOTE", note.getId(), "Добавлена заметка к студенту", Map.of("studentId", studentId));
        return toResponse(note);
    }

    @Transactional
    public NoteResponse update(UUID studentId, UUID noteId, NoteRequest request) {
        StudentNote note = requireNote(studentId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        if (request.version() == null || request.version() != note.getVersion())
            throw new ConflictException("Заметка уже изменена. Обновите данные");
        note.setBody(request.body().trim());
        note.setPinned(request.pinned());
        audit.record("NOTE_UPDATED", "STUDENT_NOTE", noteId, "Изменена заметка", Map.of("studentId", studentId));
        return toResponse(note);
    }

    @Transactional
    public void delete(UUID studentId, UUID noteId) {
        StudentNote note = requireNote(studentId, noteId);
        UserAccount actor = currentUser.requireEntity();
        assertOwnerOrAdmin(note, actor);
        notes.delete(note);
        audit.record("NOTE_DELETED", "STUDENT_NOTE", noteId, "Удалена заметка", Map.of("studentId", studentId));
    }

    private Student requireStudent(UUID id) {
        return students.findById(id).orElseThrow(() -> new NotFoundException("Студент не найден"));
    }

    private StudentNote requireNote(UUID studentId, UUID noteId) {
        StudentNote note = notes.findById(noteId).orElseThrow(() -> new NotFoundException("Заметка не найдена"));
        if (!note.getStudent().getId().equals(studentId)) throw new NotFoundException("Заметка не найдена");
        return note;
    }

    private static void assertOwnerOrAdmin(StudentNote note, UserAccount actor) {
        if (!note.getAuthor().getId().equals(actor.getId()) && !actor.getRoles().contains(Role.ADMIN)) {
            throw new AccessDeniedException("Only note author or administrator can modify it");
        }
    }

    private static NoteResponse toResponse(StudentNote note) {
        return new NoteResponse(note.getId(), note.getStudent().getId(), note.getAuthor().getId(),
                note.getAuthor().getDisplayName(), note.getBody(), note.isPinned(),
                note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}
