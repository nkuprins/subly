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

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionPayment;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class RecordPaymentServiceTest extends AbstractSubscriptionServiceTest {

    @Captor
    ArgumentCaptor<List<SubscriptionPayment>> payments;

    @Test
    void advancePaymentDates_shouldRecordEachMembersShareAndMoveOneCycle() {
        LocalDate twoMonthsAgo = TODAY.minusMonths(2);
        Subscription subscription = due("10.00", twoMonthsAgo);
        SubscriptionMembership owner = SubscriptionMembership.owner(alice, subscription);
        SubscriptionMembership bobMember = SubscriptionMembership.member(bob, subscription);
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
        return due(SUBSCRIPTION_ID, price, nextPaymentDate);
    }

    private static Subscription due(long id, String price, LocalDate nextPaymentDate) {
        return aSubscription()
                .withId(id)
                .withPrice(price)
                .withNextPaymentDate(nextPaymentDate)
                .build();
    }
}
