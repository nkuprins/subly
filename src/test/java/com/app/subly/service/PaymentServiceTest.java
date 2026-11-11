package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionPayment;
import com.app.subly.model.user.User;
import com.app.subly.repository.SubscriptionPaymentRepository;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    SubscriptionPaymentRepository paymentRepository;

    PaymentService service;

    final User alice = TestUsers.user(ALICE);
    final Subscription spotifyInDollars =
            aSubscription().withId(3L).withCurrency(Currency.USD).build();

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository);
    }

    @Test
    void findHistory_shouldReturnPaymentsWithServiceAndCurrency() {
        SubscriptionPayment payment = payment(spotifyInDollars, "4.50", TODAY.minusDays(3));
        when(paymentRepository.findHistoryOf(ALICE)).thenReturn(List.of(payment));

        assertThat(service.findHistory(ALICE))
                .containsExactly(new PaymentResponse(
                        3L,
                        spotifyInDollars.getProvider().getName(),
                        TODAY.minusDays(3),
                        new BigDecimal("4.50"),
                        Currency.USD));
    }

    private SubscriptionPayment payment(Subscription subscription, String amount, LocalDate paidOn) {
        return new SubscriptionPayment(subscription, alice, new BigDecimal(amount), paidOn);
    }
}
