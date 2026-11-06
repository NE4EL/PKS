package ru.mirea.isp.service;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;
import ru.mirea.isp.domain.*;
/** Приоритет открытой заявки: срочность и оценка трудозатрат (часы). */
@Component
public class PriorityCalculator {
 public PriorityLevel calculate(ConnectionRequestStatus status, BigDecimal hours, boolean urgent) {
  if (status == ConnectionRequestStatus.COMPLETED || status == ConnectionRequestStatus.REJECTED || status == ConnectionRequestStatus.CANCELLED) return PriorityLevel.NOT_APPLICABLE;
  boolean complex = hours != null && hours.compareTo(new BigDecimal("8")) >= 0;
  if (urgent && complex) return PriorityLevel.CRITICAL;
  if (urgent) return PriorityLevel.HIGH;
  if (hours == null || complex) return PriorityLevel.MEDIUM;
  return PriorityLevel.LOW;
 }
}
