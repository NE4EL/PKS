package ru.mirea.studenthub.fx.net;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет запрет cleartext для удалённого API. */
class ApiClientSecurityTest {
    @Test
    void permitsLocalDevelopmentHttp() {
        assertThatCode(() -> new ApiClient("http://localhost:8080/api")).doesNotThrowAnyException();
    }

    @Test
    void rejectsRemoteCleartextHttp() {
        assertThatThrownBy(() -> new ApiClient("http://example.com/api"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void permitsRemoteHttps() {
        assertThatCode(() -> new ApiClient("https://students.example.edu/api")).doesNotThrowAnyException();
    }
}
