package ru.mirea.studenthub.fx.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PhoneFormatTest {
    @Test
    void normalizesRussianMobileNumbers() {
        assertThat(PhoneFormat.normalize("89991234567")).isEqualTo("+7-999-123-45-67");
        assertThat(PhoneFormat.normalize("+7 (999) 123-45-67")).isEqualTo("+7-999-123-45-67");
        assertThat(PhoneFormat.normalize("79991234567")).isEqualTo("+7-999-123-45-67");
    }

    @Test
    void removesLettersAndLimitsDigits() {
        assertThat(PhoneFormat.normalize("abc9991234567xyz123")).isEqualTo("+7-999-123-45-67");
    }

    @Test
    void validatesOnlyCompleteMask() {
        assertThat(PhoneFormat.isComplete("+7-999-123-45-67")).isTrue();
        assertThat(PhoneFormat.isComplete("+7-999-123")).isFalse();
    }
}
