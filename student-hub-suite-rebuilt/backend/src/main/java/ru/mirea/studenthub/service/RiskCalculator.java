package ru.mirea.studenthub.service;

import org.springframework.stereotype.Component;
import ru.mirea.studenthub.domain.RiskLevel;
import ru.mirea.studenthub.domain.StudentStatus;

import java.math.BigDecimal;

/** Единообразно рассчитывает академический риск для всех клиентов. */
@Component
public class RiskCalculator {
    public RiskLevel calculate(StudentStatus status, BigDecimal average, boolean hasDebt) {
        /*
         * Низкий риск и отсутствие применимости — разные вещи.
         * Для отчисленного, выпускника или переведённого студента текущий академический
         * риск уже не рассчитывается, поэтому используется отдельное состояние.
         */
        if (status == StudentStatus.EXPELLED
                || status == StudentStatus.GRADUATED
                || status == StudentStatus.TRANSFERRED) {
            return RiskLevel.NOT_APPLICABLE;
        }
        /*
         * Используется 100-балльная шкала:
         * - ниже 60 баллов — зона повышенного академического риска;
         * - от 60 до 74.99 — средний риск;
         * - 75 и выше — низкий риск при отсутствии иных проблем.
         */
        if (hasDebt && (average == null || average.compareTo(new BigDecimal("60.00")) < 0)) return RiskLevel.CRITICAL;
        if (hasDebt || (average != null && average.compareTo(new BigDecimal("60.00")) < 0)) return RiskLevel.HIGH;
        if (average == null || average.compareTo(new BigDecimal("75.00")) < 0) return RiskLevel.MEDIUM;
        return RiskLevel.LOW;
    }
}
