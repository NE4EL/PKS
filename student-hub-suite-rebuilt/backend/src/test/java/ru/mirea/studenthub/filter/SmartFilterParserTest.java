package ru.mirea.studenthub.filter;

import org.junit.jupiter.api.Test;
import ru.mirea.studenthub.domain.RiskLevel;
import ru.mirea.studenthub.domain.StudentStatus;
import ru.mirea.studenthub.error.BadRequestException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Проверяет русские и английские команды умного фильтра. */
class SmartFilterParserTest {
    private final SmartFilterParser parser = new SmartFilterParser();

    @Test
    void parsesMixedSmartQuery() {
        StudentFilter result = parser.parse("Иванов группа:ЭФБО-07-24 балл<3,5 долг:да риск:высокий тег:староста");
        assertThat(result.freeText()).isEqualTo("Иванов");
        assertThat(result.group()).isEqualTo("ЭФБО-07-24");
        assertThat(result.status()).isNull();
        assertThat(result.debt()).isTrue();
        assertThat(result.risk()).isEqualTo(RiskLevel.HIGH);
        assertThat(result.tag()).isEqualTo("староста");
        assertThat(result.archived()).isFalse();
        assertThat(result.grades()).hasSize(1);
    }

    @Test
    void supportsQuotedFreeTextAndStatus() {
        StudentFilter result = parser.parse("\"Иван Иванов\" status:academic_leave archived:all");
        assertThat(result.freeText()).isEqualTo("Иван Иванов");
        assertThat(result.status()).isEqualTo(StudentStatus.ACADEMIC_LEAVE);
        assertThat(result.archived()).isNull();
    }

    @Test
    void rejectsOutOfRangeGrade() {
        assertThatThrownBy(() -> parser.parse("балл>7"))
                .isInstanceOf(BadRequestException.class);
    }
}
