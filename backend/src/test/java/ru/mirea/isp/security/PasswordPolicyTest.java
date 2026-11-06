package ru.mirea.isp.security;

import org.junit.jupiter.api.Test;
import ru.mirea.isp.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет минимальную серверную политику паролей. */
class PasswordPolicyTest {
    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsStrongPassword() {
        assertThatCode(() -> policy.validate("Correct-Horse-2026!", "operator@example.edu"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPasswordContainingLogin() {
        assertThatThrownBy(() -> policy.validate("Operator-Strong-2026!", "operator@example.edu"))
                .isInstanceOf(BadRequestException.class);
    }
}
