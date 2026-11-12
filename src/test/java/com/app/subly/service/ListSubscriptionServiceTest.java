package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestProviders.CATALOG_PROVIDER_NAME;
import static com.app.subly.testdata.TestProviders.CUSTOM_PROVIDER_NAME;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.ActiveSubscriptionResponse;
import com.app.subly.dto.subscription.CancelledSubscriptionResponse;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.exception.subscription.SubscriptionNotActiveException;
import com.app.subly.service.share.MemberShare;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void findActive_shouldReturnSubscriptionsWithMembershipRolesAndUsersShare() {
        SubscriptionMembership owner = SubscriptionMembership.owner(alice, subscription(customProvider()));
        SubscriptionMembership member = SubscriptionMembership.member(alice, subscription(catalogProvider()));
        when(memberShareLoader.forUser(ALICE, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(
                        new MemberShare(owner, new BigDecimal("6.00")),
                        new MemberShare(member, new BigDecimal("3.00"))));

        assertThat(service.findActive(ALICE))
                .extracting(
                        active -> active.subscription().providerName(),
                        ActiveSubscriptionResponse::role,
                        ActiveSubscriptionResponse::share)
                .containsExactly(
                        tuple(CUSTOM_PROVIDER_NAME, MembershipRole.OWNER, new BigDecimal("6.00")),
                        tuple(CATALOG_PROVIDER_NAME, MembershipRole.MEMBER, new BigDecimal("3.00")));
    }

    @Test
    void getActive_shouldReturnOneActiveSubscriptionOfTheUser() {
        Subscription subscription = aSubscription().withId(SUBSCRIPTION_ID).build();
        stubMembership(SubscriptionMembership.member(alice, subscription));

        assertThat(service.getActive(ALICE, SUBSCRIPTION_ID))
                .isEqualTo(new SubscriptionResponse(
                        SUBSCRIPTION_ID,
                        CATALOG_PROVIDER_NAME,
                        DEFAULT_PRICE,
                        Currency.EUR,
                        BillingCycle.MONTHLY,
                        DEFAULT_NEXT_PAYMENT_DATE,
                        false,
                        1));
    }

    @Test
    void getActive_shouldRejectCancelledSubscription() {
        stubMembership(SubscriptionMembership.owner(alice, cancelled(SUBSCRIPTION_ID, TODAY.plusDays(3))));

        assertThatThrownBy(() -> service.getActive(ALICE, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotActiveException.class);
    }

    @Test
    void findCancelled_shouldReturnEveryCancelledSubscriptionWithItsSavingsSoFar() {
        SubscriptionMembership ended = SubscriptionMembership.owner(alice, cancelled(1L, TODAY.minusMonths(1)));
        SubscriptionMembership endsLater = SubscriptionMembership.owner(alice, cancelled(2L, TODAY.plusDays(3)));
        stubAliceExists();
        when(memberShareLoader.forUser(ALICE, SubscriptionStatus.CANCELLED))
                .thenReturn(List.of(
                        new MemberShare(ended, new BigDecimal("5.00")),
                        new MemberShare(endsLater, new BigDecimal("5.00"))));

        assertThat(service.findCancelled(ALICE))
                .extracting(cancelled -> cancelled.subscription().id(), CancelledSubscriptionResponse::saved)
                .containsExactly(tuple(1L, new BigDecimal("10.00")), tuple(2L, new BigDecimal("0.00")));
    }

    private void stubMembership(SubscriptionMembership membership) {
        when(subscriptionAccess.getMembership(ALICE, SUBSCRIPTION_ID)).thenReturn(membership);
    }

    private static Subscription cancelled(long id, LocalDate endDate) {
        return aSubscription()
                .withId(id)
                .withNextPaymentDate(endDate)
                .cancelled()
                .build();
    }
}
