package ru.mirea.studenthub.android.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PhoneFormatterTest {
    @Test
    public void formatsRussianMobileFromEight() {
        assertEquals("+7-999-123-45-67", PhoneFormatter.format("89991234567"));
    }

    @Test
    public void removesLettersAndNoise() {
        assertEquals("+7-999-123-45-67", PhoneFormatter.format("+7 abc 999 123 45 67"));
    }

    @Test
    public void validatesOnlyCompleteMask() {
        assertTrue(PhoneFormatter.isComplete("+7-999-123-45-67"));
        assertFalse(PhoneFormatter.isComplete("+7-999-123"));
    }
}
