package ru.mirea.studenthub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.studenthub.domain.StudentNote;

import java.util.List;
import java.util.UUID;

/** Persistence gateway for student notes. */
public interface StudentNoteRepository extends JpaRepository<StudentNote, UUID> {
    List<StudentNote> findByStudentIdOrderByPinnedDescCreatedAtDesc(UUID studentId);
}
