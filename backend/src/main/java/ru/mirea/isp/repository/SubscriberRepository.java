package ru.mirea.isp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.isp.domain.Subscriber;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for subscribers. */
public interface SubscriberRepository extends JpaRepository<Subscriber, UUID> {
    Optional<Subscriber> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<Subscriber> findAllByOrderByCodeAsc();
    List<Subscriber> findByActiveTrueOrderByCodeAsc();
}
