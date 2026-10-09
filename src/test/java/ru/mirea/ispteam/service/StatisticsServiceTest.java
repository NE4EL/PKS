package ru.mirea.ispteam.service;

import org.junit.jupiter.api.Test;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.RequestType;
import ru.mirea.ispteam.model.Subscriber;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class StatisticsServiceTest {

    @Test
    void buildsAllIndicatorsFromOneSnapshot() {
        TestData data = new TestData();
        Subscriber a = data.subscriber("Иванов Иван", "+79001112233");
        Subscriber b = data.subscriber("Петрова Мария", "+79004445566");
        LocalDateTime t = LocalDateTime.of(2024, 5, 1, 10, 0);
        data.request(a, RequestType.NEW_CONNECTION, RequestStatus.COMPLETED, "Домашний 100", "Подключение",
                t, t.plusHours(10));
        data.request(a, RequestType.TARIFF_CHANGE, RequestStatus.COMPLETED, "Домашний 100", "Смена тарифа",
                t.plusDays(10), t.plusDays(10).plusHours(30));
        data.request(b, RequestType.TECHNICAL_SUPPORT, RequestStatus.NEW, null, "Диагностика",
                t.plusDays(40), t.plusDays(40));
        data.request(b, RequestType.DISCONNECTION, RequestStatus.CANCELLED, null, "Отключение",
                t.plusDays(41), t.plusDays(42));

        StatisticsService service = new StatisticsService(data.subscribers, data.requests);
        StatisticsReport report = service.buildReport(LocalDate.of(2024, 5, 1), LocalDate.of(2024, 5, 31));

        assertThat(report.totalSubscribers()).isEqualTo(2);
        assertThat(report.totalRequests()).isEqualTo(4);
        assertThat(report.activeRequests()).isEqualTo(1);
        assertThat(report.completedRequests()).isEqualTo(2);
        assertThat(report.cancelledOrRejected()).isEqualTo(1);
        assertThat(report.completedPercent()).isCloseTo(50.0, within(0.001));
        assertThat(report.avgRequestsPerSubscriber()).isCloseTo(2.0, within(0.001));
        assertThat(report.avgProcessingHours().getAsDouble()).isCloseTo(20.0, within(0.001));
        assertThat(report.minProcessingHours().getAsDouble()).isCloseTo(10.0, within(0.001));
        assertThat(report.maxProcessingHours().getAsDouble()).isCloseTo(30.0, within(0.001));
        assertThat(report.mostPopularTariff()).isEqualTo("Домашний 100");
        assertThat(report.byStatus()).containsEntry(RequestStatus.COMPLETED, 2L)
                .containsEntry(RequestStatus.REJECTED, 0L).hasSize(RequestStatus.values().length);
        assertThat(report.byType()).containsEntry(RequestType.EQUIPMENT_REPLACEMENT, 0L);
        assertThat(report.requestsInPeriod()).isEqualTo(2);
    }

    @Test
    void emptyDatabaseGivesZerosWithoutDivisionByZero() {
        TestData data = new TestData();
        StatisticsReport report = new StatisticsService(data.subscribers, data.requests).buildReport(null, null);
        assertThat(report.completedPercent()).isZero();
        assertThat(report.avgRequestsPerSubscriber()).isZero();
        assertThat(report.avgProcessingHours()).isEmpty();
        assertThat(report.mostPopularTariff()).isNull();
    }

    @Test
    void reversedPeriodIsValidationError() {
        TestData data = new TestData();
        StatisticsService service = new StatisticsService(data.subscribers, data.requests);
        assertThatThrownBy(() -> service.buildReport(LocalDate.of(2024, 2, 1), LocalDate.of(2024, 1, 1)))
                .isInstanceOf(ValidationException.class);
    }
}
