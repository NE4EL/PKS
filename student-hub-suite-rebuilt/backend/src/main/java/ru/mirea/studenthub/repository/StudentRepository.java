package ru.mirea.studenthub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.studenthub.domain.Student;
import ru.mirea.studenthub.domain.StudentStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for students, smart specifications and compact analytics. */
public interface StudentRepository extends JpaRepository<Student, UUID>, JpaSpecificationExecutor<Student> {
    boolean existsByExternalIdIgnoreCase(String externalId);
    boolean existsByExternalIdIgnoreCaseAndIdNot(String externalId, UUID id);
    Optional<Student> findByExternalIdIgnoreCase(String externalId);
    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndHasDebtTrue();
    long countByArchivedFalseAndStatus(StudentStatus status);

    @Query("select coalesce(avg(s.averageGrade), 0) from Student s where s.archived = false and s.averageGrade is not null")
    BigDecimal averageGradeForActive();

    @Query("select coalesce(g.code, 'Без группы'), count(s) from Student s left join s.group g " +
           "where s.archived = false group by g.code order by count(s) desc")
    List<Object[]> countActiveByGroup();

    @Query("select s.status, count(s) from Student s where s.archived = false group by s.status order by count(s) desc")
    List<Object[]> countActiveByStatus();

    @Query("select s.riskLevel, count(s) from Student s where s.archived = false group by s.riskLevel order by count(s) desc")
    List<Object[]> countActiveByRisk();
}
