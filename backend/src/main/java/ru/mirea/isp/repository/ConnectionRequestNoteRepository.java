package ru.mirea.isp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.isp.domain.ConnectionRequestNote;

import java.util.List;
import java.util.UUID;

/** Persistence gateway for request notes. */
public interface ConnectionRequestNoteRepository extends JpaRepository<ConnectionRequestNote, UUID> {
    List<ConnectionRequestNote> findByRequestIdOrderByPinnedDescCreatedAtDesc(UUID requestId);
}
