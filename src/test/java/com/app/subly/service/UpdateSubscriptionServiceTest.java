package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestProviders.CATALOG_PROVIDER_NAME;
import static com.app.subly.testdata.TestProviders.CUSTOM_PROVIDER_NAME;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void update_shouldChangePaymentDetailsAndKeepProvider() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnerRowWithSubscriptionDetails(subscription);
        UpdateSubscriptionRequest request = updateRequest();

        SubscriptionResponse response = subscriptionService.update(ALICE, SUBSCRIPTION_ID, request);

        assertPaymentDetails(subscription, request);
        assertThat(subscription.getProvider().getName()).isEqualTo(CATALOG_PROVIDER_NAME);
        assertThat(response).isEqualTo(SubscriptionResponse.from(subscription));
        verifyNoInteractions(userRepository);
    }

    @Test
    void update_shouldRejectNonOwner() {
        Subscription subscription = subscription(customProvider());
        stubOwnerRowWithSubscriptionDetails(subscription);
        UpdateSubscriptionRequest request = updateRequest();

        assertThatThrownBy(() -> subscriptionService.update(BOB, SUBSCRIPTION_ID, request))
                .isInstanceOf(SubscriptionAccessDeniedException.class);
        assertUnchanged(subscription);
    }

    @Test
    void update_shouldRejectUnknownSubscription() {
        when(membershipRepository.findWithSubscriptionDetailsBySubscriptionIdAndRole(
                        SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.empty());
        UpdateSubscriptionRequest request = updateRequest();

        assertThatThrownBy(() -> subscriptionService.update(ALICE, SUBSCRIPTION_ID, request))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }

    private static UpdateSubscriptionRequest updateRequest() {
        return new UpdateSubscriptionRequest(new BigDecimal("19.99"), BillingCycle.YEARLY, TODAY.plusDays(3), true);
    }

    private void assertUnchanged(Subscription subscription) {
        assertThat(subscription.getProvider().getName()).isEqualTo(CUSTOM_PROVIDER_NAME);
        assertThat(subscription.getProvider().getCategory()).isSameAs(category);
        assertThat(subscription.getPrice()).isEqualByComparingTo(DEFAULT_PRICE);
    }
}
