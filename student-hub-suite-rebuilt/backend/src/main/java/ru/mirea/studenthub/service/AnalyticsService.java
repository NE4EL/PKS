package ru.mirea.studenthub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.mirea.studenthub.dto.AnalyticsDtos.AnalyticsResponse;
import ru.mirea.studenthub.dto.AnalyticsDtos.NamedCount;
import ru.mirea.studenthub.repository.StudentRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Формирует компактную аналитическую сводку на стороне базы. */
@Service
public class AnalyticsService {
    private final StudentRepository students;

    public AnalyticsService(StudentRepository students) {
        this.students = students;
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse summary() {
        long total = students.count();
        long active = students.countByArchivedFalse();
        long archived = students.countByArchivedTrue();
        long withDebt = students.countByArchivedFalseAndHasDebtTrue();
        BigDecimal average = students.averageGradeForActive();
        if (average != null) average = average.setScale(2, RoundingMode.HALF_UP);
        return new AnalyticsResponse(total, active, archived, withDebt, average,
                map(students.countActiveByGroup()), map(students.countActiveByStatus()), map(students.countActiveByRisk()));
    }

    private static List<NamedCount> map(List<Object[]> rows) {
        return rows.stream().map(row -> new NamedCount(String.valueOf(row[0]), ((Number) row[1]).longValue())).toList();
    }
}
