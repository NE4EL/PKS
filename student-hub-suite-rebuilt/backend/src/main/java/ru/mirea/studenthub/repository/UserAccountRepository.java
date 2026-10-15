package ru.mirea.studenthub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.mirea.studenthub.domain.UserAccount;

import java.util.Optional;
import java.util.UUID;

/** Persistence gateway for user accounts. */
public interface UserAccountRepository extends JpaRepository<UserAccount, UUID> {
    Optional<UserAccount> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
}
