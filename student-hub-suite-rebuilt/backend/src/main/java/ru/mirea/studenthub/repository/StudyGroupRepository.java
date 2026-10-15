package ru.mirea.studenthub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.studenthub.domain.StudyGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for study groups. */
public interface StudyGroupRepository extends JpaRepository<StudyGroup, UUID> {
    Optional<StudyGroup> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<StudyGroup> findAllByOrderByCodeAsc();
    List<StudyGroup> findByActiveTrueOrderByCodeAsc();
}
