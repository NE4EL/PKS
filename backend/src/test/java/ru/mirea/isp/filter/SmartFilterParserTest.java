package ru.mirea.isp.filter;

import org.junit.jupiter.api.Test;
import ru.mirea.isp.domain.PriorityLevel;
import ru.mirea.isp.domain.ConnectionRequestStatus;
import ru.mirea.isp.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет русские и английские команды умного фильтра. */
class SmartFilterParserTest {
    private final SmartFilterParser parser = new SmartFilterParser();

    @Test
    void parsesMixedSmartQuery() {
        ConnectionRequestFilter result = parser.parse("Иванов абонент:ACC-001 часы<3,5 срочно:да приоритет:высокий тег:подключение");
        assertThat(result.freeText()).isEqualTo("Иванов");
        assertThat(result.subscriber()).isEqualTo("ACC-001");
        assertThat(result.status()).isNull();
        assertThat(result.urgent()).isTrue();
        assertThat(result.priority()).isEqualTo(PriorityLevel.HIGH);
        assertThat(result.tag()).isEqualTo("подключение");
        assertThat(result.archived()).isFalse();
        assertThat(result.hours()).hasSize(1);
    }

    @Test
    void supportsQuotedFreeTextAndStatus() {
        ConnectionRequestFilter result = parser.parse("\"Иван Иванов\" status:IN_PROGRESS archived:all");
        assertThat(result.freeText()).isEqualTo("Иван Иванов");
        assertThat(result.status()).isEqualTo(ConnectionRequestStatus.IN_PROGRESS);
        assertThat(result.archived()).isNull();
    }

    @Test
    void rejectsOutOfRangeHours() {
        assertThatThrownBy(() -> parser.parse("часы>101"))
                .isInstanceOf(BadRequestException.class);
    }
}
