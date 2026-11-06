package ru.mirea.isp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.isp.domain.UserAccount;
import ru.mirea.isp.repository.UserAccountRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Управляет блокировкой перебора пароля.
 *
 * <p>Неудачная попытка записывается в отдельной транзакции, чтобы откат основного
 * запроса не сбрасывал счётчик попыток входа.</p>
 */
@Service
public class LoginAttemptService {
    private static final int MAX_FAILURES = 5;
    private final UserAccountRepository users;
    private final AuditService audit;

    public LoginAttemptService(UserAccountRepository users, AuditService audit) {
        this.users = users;
        this.audit = audit;
    }

    /** Фиксирует неудачу и включает временную блокировку после порога. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(UUID userId) {
        users.findById(userId).ifPresent(user -> {
            int failures = user.getFailedLoginAttempts() + 1;
            user.setFailedLoginAttempts(failures);
            audit.recordActor(user.getId(), user.getEmail(), "LOGIN_FAILURE", "USER", userId, "Неудачная попытка входа", java.util.Map.of());
            if (failures >= MAX_FAILURES) {
                int multiplier = Math.min(8, failures - MAX_FAILURES + 1);
                user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(5L * multiplier)));
            }
        });
    }

    /** Сбрасывает счётчик только после успешной проверки пароля. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSuccess(UUID userId) {
        users.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
        });
    }

    /** Проверяет, действует ли блокировка. */
    public boolean isLocked(UserAccount user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }
}
