package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.PaidMonthResponse;
import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionPayment;
import com.app.subly.model.user.User;
import com.app.subly.repository.SubscriptionPaymentRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestClock;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    static final YearMonth THIS_MONTH = YearMonth.from(TODAY);
    static final YearMonth FIRST_MONTH = THIS_MONTH.minusMonths(11);

    @Mock
    SubscriptionPaymentRepository paymentRepository;

    @Mock
    UserRepository userRepository;

    PaymentService service;

    final User alice = TestUsers.user(ALICE);
    final Subscription yearlyGym =
            aSubscription().withId(1L).withBillingCycle(BillingCycle.YEARLY).build();
    final Subscription netflix = aSubscription().withId(2L).build();
    final Subscription spotifyInDollars =
            aSubscription().withId(3L).withCurrency(Currency.USD).build();

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, userRepository, TestClock.CLOCK);
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

    @Test
    void getPaidPerMonth_shouldReturnTwelveMonthsWithOneEuroTotalEach() {
        when(userRepository.getByUsername(ALICE)).thenReturn(TestUsers.user(ALICE));
        when(paymentRepository.findPaidSince(ALICE, FIRST_MONTH.atDay(1)))
                .thenReturn(List.of(
                        payment(yearlyGym, "5.00", FIRST_MONTH.atDay(1)),
                        payment(netflix, "4.00", THIS_MONTH.atDay(1)),
                        payment(spotifyInDollars, "2.00", TODAY)));

        List<PaidMonthResponse> result = service.getPaidPerMonth(ALICE, Currency.EUR);

        assertThat(result).hasSize(12);
        assertThat(result.getFirst())
                .isEqualTo(new PaidMonthResponse(FIRST_MONTH, new MoneyResponse(new BigDecimal("5.00"), Currency.EUR)));
        assertThat(result.get(1))
                .isEqualTo(new PaidMonthResponse(
                        FIRST_MONTH.plusMonths(1), new MoneyResponse(new BigDecimal("0.00"), Currency.EUR)));
        // Fails if conversion stops using current 0.89
        // 4.00 EUR + 2.00 USD * 0.89
        assertThat(result.getLast())
                .isEqualTo(new PaidMonthResponse(THIS_MONTH, new MoneyResponse(new BigDecimal("5.78"), Currency.EUR)));
    }

    private SubscriptionPayment payment(Subscription subscription, String amount, LocalDate paidOn) {
        return new SubscriptionPayment(subscription, alice, new BigDecimal(amount), paidOn);
    }
}
