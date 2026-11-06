package ru.mirea.isp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import ru.mirea.isp.domain.ConnectionRequest;
import ru.mirea.isp.domain.ConnectionRequestStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for requests, smart specifications and compact analytics. */
public interface ConnectionRequestRepository extends JpaRepository<ConnectionRequest, UUID>, JpaSpecificationExecutor<ConnectionRequest> {
    boolean existsByExternalIdIgnoreCase(String externalId);
    boolean existsByExternalIdIgnoreCaseAndIdNot(String externalId, UUID id);
    Optional<ConnectionRequest> findByExternalIdIgnoreCase(String externalId);
    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndUrgentTrue();
    long countByArchivedFalseAndStatus(ConnectionRequestStatus status);

    @Query("select coalesce(avg(s.estimatedHours), 0) from ConnectionRequest s where s.archived = false and s.estimatedHours is not null")
    BigDecimal estimatedHoursForActive();

    @Query("select coalesce(g.code, 'Без абонента'), count(s) from ConnectionRequest s left join s.subscriber g " +
           "where s.archived = false group by g.code order by count(s) desc")
    List<Object[]> countActiveBySubscriber();

    @Query("select s.status, count(s) from ConnectionRequest s where s.archived = false group by s.status order by count(s) desc")
    List<Object[]> countActiveByStatus();

    @Query("select s.priorityLevel, count(s) from ConnectionRequest s where s.archived = false group by s.priorityLevel order by count(s) desc")
    List<Object[]> countActiveByPriority();
}
