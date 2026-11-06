package ru.mirea.ispteam.service;

import org.junit.jupiter.api.Test;
import ru.mirea.ispteam.exception.ValidationException;
import ru.mirea.ispteam.model.RequestStatus;
import ru.mirea.ispteam.model.Subscriber;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BusinessRulesTest {

    /** Автономные проверки участника B (186 сценариев) запускаются вместе с mvn test. */
    @Test
    void serviceChecksOfRoleBPass() {
        ServiceChecks.main(new String[0]);
    }

    @Test
    void allowedTransitionsMatchTableFromSpec() {
        assertThat(ConnectionRequestService.allowedTransitions(RequestStatus.NEW))
                .containsExactlyInAnyOrder(RequestStatus.IN_PROGRESS, RequestStatus.REJECTED, RequestStatus.CANCELLED);
        assertThat(ConnectionRequestService.allowedTransitions(RequestStatus.APPROVED))
                .containsExactlyInAnyOrder(RequestStatus.COMPLETED, RequestStatus.CANCELLED);
        assertThat(ConnectionRequestService.allowedTransitions(RequestStatus.COMPLETED)).isEmpty();
        assertThat(ConnectionRequestService.allowedTransitions(null)).isEmpty();
    }

    @Test
    void validationErrorNamesTheField() {
        TestData data = new TestData();
        Subscriber badPhone = new Subscriber("Иванов Иван", "8900", null, "Москва", null);
        assertThatThrownBy(() -> data.subscribers.create(badPhone))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("phone");
        Subscriber noAddress = new Subscriber("Иванов Иван", "+79001112233", null, " ", null);
        assertThatThrownBy(() -> data.subscribers.create(noAddress))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("address");
    }

    @Test
    void registrationDateCannotBeInFuture() {
        TestData data = new TestData();
        Subscriber future = new Subscriber("Иванов Иван", "+79001112233", null, "Москва", LocalDate.now().plusDays(1));
        assertThatThrownBy(() -> data.subscribers.create(future))
                .isInstanceOf(ValidationException.class).extracting("field").isEqualTo("registrationDate");
        assertThat(data.subscribers.getAll()).isEmpty();
    }
}
