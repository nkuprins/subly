package com.app.subly.service;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.CAROL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.subscription.SubscriptionPayment;
import com.app.subly.entity.user.User;
import com.app.subly.enums.Currency;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionPaymentRepository;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private SubscriptionPaymentRepository paymentRepository;

    @Mock
    private SubscriptionMembershipRepository membershipRepository;

    @InjectMocks
    private PaymentService service;

    @Captor
    ArgumentCaptor<List<SubscriptionPayment>> payments;

    private final User alice = TestUsers.user(ALICE);
    private final Subscription subscriptionInDollars =
            aSubscription().withCurrency(Currency.USD).build();

    @Test
    void findHistory_shouldReturnPaymentsWithServiceAndCurrency() {
        SubscriptionPayment payment = payment(subscriptionInDollars, TODAY.minusDays(3));
        when(paymentRepository.findHistoryOf(ALICE)).thenReturn(List.of(payment));

        assertThat(service.findHistory(ALICE))
                .containsExactly(new PaymentResponse(
                        subscriptionInDollars.getProvider().getName(),
                        TODAY.minusDays(3),
                        new BigDecimal("4.50"),
                        Currency.USD));
    }

    @Test
    void advancePaymentDates_shouldRecordAPaymentForEachMemberAndMoveOneCycle() {
        Subscription subscription = aSubscription()
                .withId(SUBSCRIPTION_ID)
                .withNextPaymentDate(TODAY.minusMonths(2))
                .build();
        SubscriptionMembership owner = SubscriptionMembership.owner(alice, subscription);
        SubscriptionMembership bobMember = SubscriptionMembership.member(TestUsers.user(BOB), subscription);
        SubscriptionMembership carolMember = SubscriptionMembership.member(TestUsers.user(CAROL), subscription);
        stubDue(owner, List.of(owner, bobMember, carolMember));

        service.advancePaymentDates(DEFAULT_ZONE, TODAY);

        verify(paymentRepository).saveAll(payments.capture());
        assertThat(payments.getValue())
                .extracting(payment -> payment.getUser().getUsername())
                .containsExactlyInAnyOrder(ALICE, BOB, CAROL);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(TODAY.minusMonths(1));
    }

    private SubscriptionPayment payment(Subscription subscription, LocalDate paidOn) {
        return new SubscriptionPayment(subscription, alice, new BigDecimal("4.50"), paidOn);
    }

    private void stubDue(SubscriptionMembership owner, List<SubscriptionMembership> members) {
        when(membershipRepository.findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                        MembershipRole.OWNER, SubscriptionStatus.ACTIVE, DEFAULT_ZONE, TODAY))
                .thenReturn(List.of(owner));
        when(membershipRepository.findBySubscriptionIdOrderByIdAsc(SUBSCRIPTION_ID))
                .thenReturn(members);
    }
}
