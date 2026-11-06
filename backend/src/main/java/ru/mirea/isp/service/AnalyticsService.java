package ru.mirea.isp.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.isp.dto.AnalyticsDtos.AnalyticsResponse;
import ru.mirea.isp.dto.AnalyticsDtos.NamedCount;
import ru.mirea.isp.repository.ConnectionRequestRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Формирует компактную аналитическую сводку на стороне базы. */
@Service
public class AnalyticsService {
    private final ConnectionRequestRepository requests;

    public AnalyticsService(ConnectionRequestRepository requests) {
        this.requests = requests;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        long total = requests.count();
        long active = requests.countByArchivedFalse();
        long archived = requests.countByArchivedTrue();
        long urgent = requests.countByArchivedFalseAndUrgentTrue();
        BigDecimal average = requests.estimatedHoursForActive();
        if (average != null) average = average.setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(total, active, archived, urgent, average,
                map(requests.countActiveBySubscriber()), map(requests.countActiveByStatus()), map(requests.countActiveByPriority()));
    }

    private static List<NamedCount> map(List<Object[]> rows) {
        return rows.stream().map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue())).toList();
    }
}
