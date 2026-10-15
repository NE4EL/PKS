package ru.mirea.studenthub.service;

import org.junit.jupiter.api.Test;
import ru.mirea.studenthub.domain.RiskLevel;
import ru.mirea.studenthub.domain.StudentStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

/** Фиксирует бизнес-правила академического риска. */
class RiskCalculatorTest {
    private final RiskCalculator calculator = new RiskCalculator();

    @Test
    void debtAndLowGradeAreCritical() {
        assertThat(calculator.calculate(StudentStatus.ENROLLED, new BigDecimal("2.90"), true))
                .isEqualTo(RiskLevel.CRITICAL);
    }

    @Test
    void goodGradeWithoutDebtIsLow() {
        assertThat(calculator.calculate(StudentStatus.ENROLLED, new BigDecimal("4.40"), false))
                .isEqualTo(RiskLevel.LOW);
    }

    @Test
    void graduatedStudentIsNotAcademicRisk() {
        assertThat(calculator.calculate(StudentStatus.GRADUATED, new BigDecimal("2.10"), true))
                .isEqualTo(RiskLevel.LOW);
    }
}
