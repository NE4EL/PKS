package ru.mirea.studenthub.android.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PasswordValidatorTest {
    @Test
    public void acceptsStrongPassword() {
        assertNull(PasswordValidator.validate("Strong!Pass2026", "admin@example.org"));
    }

    @Test
    public void rejectsWeakPassword() {
        assertNotNull(PasswordValidator.validate("password", "admin@example.org"));
    }
}
