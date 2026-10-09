package ru.mirea.ispteam.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.ConnectionRequest;
import ru.mirea.ispteam.model.Subscriber;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static ru.mirea.ispteam.model.RequestStatus.COMPLETED;
import static ru.mirea.ispteam.model.RequestStatus.NEW;
import static ru.mirea.ispteam.model.RequestType.NEW_CONNECTION;
import static ru.mirea.ispteam.model.RequestType.TECHNICAL_SUPPORT;

class RequestFilterTest {

    private TestData data;

    @BeforeEach
    void setUp() {
        data = new TestData();
        Subscriber ivanov = data.subscriber("Иванов Иван", "+79001112233");
        Subscriber petrova = data.subscriber("Петрова Мария", "+79004445566");
        data.request(ivanov, NEW_CONNECTION, COMPLETED, "Домашний 100", "Подключение по адресу",
                LocalDateTime.of(2024, 1, 10, 10, 0), LocalDateTime.of(2024, 1, 12, 10, 0));
        data.request(ivanov, TECHNICAL_SUPPORT, NEW, null, "Медленный интернет вечером",
                LocalDateTime.of(2024, 3, 5, 18, 0), LocalDateTime.of(2024, 3, 5, 18, 0));
        data.request(petrova, TECHNICAL_SUPPORT, NEW, null, "Не работает роутер",
                LocalDateTime.of(2024, 3, 20, 9, 0), LocalDateTime.of(2024, 3, 20, 9, 0));
    }

    /** То же, что делает FilteredList в JavaFX: оставляет заявки, для которых matches() = true. */
    private List<ConnectionRequest> apply(RequestFilter filter) {
        filter.validate();
        return data.requests.getAll().stream()
                .filter(r -> filter.matches(r, data.subscribers.getById(r.getSubscriberId())))
                .toList();
    }

    @Test
    void emptyFilterMatchesEverything() {
        assertThat(apply(RequestFilter.EMPTY)).hasSize(3);
    }

    @Test
    void searchesByNamePhoneDescriptionAndTariffIgnoringCase() {
        assertThat(apply(new RequestFilter("иванов", null, null, null, null))).hasSize(2);
        assertThat(apply(new RequestFilter("4445566", null, null, null, null))).hasSize(1);
        assertThat(apply(new RequestFilter("РОУТЕР", null, null, null, null))).hasSize(1);
        assertThat(apply(new RequestFilter("домашний", null, null, null, null))).hasSize(1);
        assertThat(apply(new RequestFilter("нет такого", null, null, null, null))).isEmpty();
    }

    @Test
    void filtersCombineWithAnd() {
        assertThat(apply(new RequestFilter(null, NEW, null, null, null))).hasSize(2);
        assertThat(apply(new RequestFilter(null, NEW, TECHNICAL_SUPPORT, null, null))).hasSize(2);
        assertThat(apply(new RequestFilter("иванов", NEW, TECHNICAL_SUPPORT, null, null)))
                .extracting(ConnectionRequest::getDescription)
                .containsExactly("Медленный интернет вечером");
    }

    @Test
    void periodBoundsAreInclusive() {
        RequestFilter march = new RequestFilter(null, null, null, LocalDate.of(2024, 3, 5), LocalDate.of(2024, 3, 20));
        assertThat(apply(march)).hasSize(2);
        RequestFilter onlyFrom = new RequestFilter(null, null, null, LocalDate.of(2024, 3, 6), null);
        assertThat(apply(onlyFrom)).hasSize(1);
    }

    @Test
    void invalidPeriodIsRejected() {
        RequestFilter reversed = new RequestFilter(null, null, null, LocalDate.of(2024, 4, 1), LocalDate.of(2024, 3, 1));
        assertThatThrownBy(reversed::validate)
                .isInstanceOf(ValidationException.class)
                .extracting("field").isEqualTo("period");
    }

    @Test
    void subscriberFilterSearchesContactsAndActiveFlag() {
        Subscriber s = new Subscriber("Сидоров Пётр", "+79161112233", "petr@example.com", "ул. Тверская", null);
        assertThat(new SubscriberFilter("тверская", false).matches(s, 0)).isTrue();
        assertThat(new SubscriberFilter("PETR@", false).matches(s, 0)).isTrue();
        assertThat(new SubscriberFilter(null, true).matches(s, 0)).isFalse();
        assertThat(new SubscriberFilter(null, true).matches(s, 2)).isTrue();
    }
}
