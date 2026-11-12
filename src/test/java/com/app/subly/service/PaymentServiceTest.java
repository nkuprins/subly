package com.app.subly.service;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.CAROL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final long SUBSCRIPTION_ID = 7L;

    @Mock
    SubscriptionPaymentRepository paymentRepository;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @Captor
    ArgumentCaptor<List<SubscriptionPayment>> payments;

    PaymentService service;

    final User alice = TestUsers.user(ALICE);
    final Subscription spotifyInDollars =
            aSubscription().withId(3L).withCurrency(Currency.USD).build();

    @BeforeEach
    void setUp() {
        service = new PaymentService(paymentRepository, membershipRepository);
    }

    @Test
    void findHistory_shouldReturnPaymentsWithServiceAndCurrency() {
        SubscriptionPayment payment = payment(spotifyInDollars, "4.50", TODAY.minusDays(3));
        when(paymentRepository.findHistoryOf(ALICE)).thenReturn(List.of(payment));

        assertThat(service.findHistory(ALICE))
                .containsExactly(new PaymentResponse(
                        spotifyInDollars.getProvider().getName(),
                        TODAY.minusDays(3),
                        new BigDecimal("4.50"),
                        Currency.USD));
    }

    @Test
    void advancePaymentDates_shouldRecordEachMembersShareAndMoveOneCycle() {
        LocalDate twoMonthsAgo = TODAY.minusMonths(2);
        Subscription subscription = due("10.00", twoMonthsAgo);
        SubscriptionMembership owner = SubscriptionMembership.owner(alice, subscription);
        SubscriptionMembership bobMember = SubscriptionMembership.member(TestUsers.user(BOB), subscription);
        SubscriptionMembership carolMember = SubscriptionMembership.member(TestUsers.user(CAROL), subscription);
        stubDue(owner, bobMember, carolMember);

        service.advancePaymentDates(DEFAULT_ZONE, TODAY);

        verify(paymentRepository).saveAll(payments.capture());
        assertThat(payments.getValue())
                .extracting(
                        payment -> payment.getUser().getUsername(),
                        SubscriptionPayment::getPaidOn,
                        SubscriptionPayment::getAmount)
                .containsExactlyInAnyOrder(
                        tuple(ALICE, twoMonthsAgo, new BigDecimal("3.34")),
                        tuple(BOB, twoMonthsAgo, new BigDecimal("3.33")),
                        tuple(CAROL, twoMonthsAgo, new BigDecimal("3.33")));
        assertThat(subscription.getNextPaymentDate()).isEqualTo(TODAY.minusMonths(1));
    }

    @Test
    void advancePaymentDates_shouldRecordNothingWhenNothingIsDue() {
        stubDueOwners();

        service.advancePaymentDates(DEFAULT_ZONE, TODAY);

        verifyNoInteractions(paymentRepository);
    }

    private SubscriptionPayment payment(Subscription subscription, String amount, LocalDate paidOn) {
        return new SubscriptionPayment(subscription, alice, new BigDecimal(amount), paidOn);
    }

    private void stubDue(SubscriptionMembership owner, SubscriptionMembership... members) {
        stubDueOwners(owner);
        List<SubscriptionMembership> all = new ArrayList<>(List.of(members));
        all.addFirst(owner);
        when(membershipRepository.findBySubscriptionIdOrderByIdAsc(SUBSCRIPTION_ID))
                .thenReturn(all);
    }

    private void stubDueOwners(SubscriptionMembership... owners) {
        when(membershipRepository.findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                        MembershipRole.OWNER, SubscriptionStatus.ACTIVE, DEFAULT_ZONE, TODAY))
                .thenReturn(List.of(owners));
    }

    private static Subscription due(String price, LocalDate nextPaymentDate) {
        return aSubscription()
                .withId(SUBSCRIPTION_ID)
                .withPrice(price)
                .withNextPaymentDate(nextPaymentDate)
                .build();
    }
}
