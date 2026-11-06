package ru.mirea.isp.service;
import org.junit.jupiter.api.Test;
import ru.mirea.isp.domain.*;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
class PriorityCalculatorTest {
 private final PriorityCalculator calculator = new PriorityCalculator();
 @Test void urgentComplexRequestIsCritical() {
  assertThat(calculator.calculate(ConnectionRequestStatus.NEW, new BigDecimal("8"), true)).isEqualTo(PriorityLevel.CRITICAL);
 }
 @Test void urgentSimpleRequestIsHigh() {
  assertThat(calculator.calculate(ConnectionRequestStatus.NEW, new BigDecimal("2"), true)).isEqualTo(PriorityLevel.HIGH);
 }
 @Test void simpleRequestIsLow() {
  assertThat(calculator.calculate(ConnectionRequestStatus.NEW, new BigDecimal("4"), false)).isEqualTo(PriorityLevel.LOW);
 }
 @Test void missingEstimateIsMedium() {
  assertThat(calculator.calculate(ConnectionRequestStatus.NEW, null, false)).isEqualTo(PriorityLevel.MEDIUM);
 }
 @Test void closedRequestHasNoPriority() {
  for (var status : new ConnectionRequestStatus[]{ConnectionRequestStatus.COMPLETED,ConnectionRequestStatus.REJECTED,ConnectionRequestStatus.CANCELLED})
   assertThat(calculator.calculate(status, new BigDecimal("10"), true)).isEqualTo(PriorityLevel.NOT_APPLICABLE);
 }
}
