package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Subscription;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void update_shouldChangePaymentDetailsAndKeepProvider() {
        SubscriptionProvider provider = catalogProvider();
        Subscription subscription = subscription(provider);
        stubOwnerMembership(subscription);
        UpdateSubscriptionRequest request = updateRequest();

        service.update(ALICE, SUBSCRIPTION_ID, request);

        assertPaymentDetails(subscription, request);
        assertThat(subscription.getProvider()).isSameAs(provider);
    }

    private static UpdateSubscriptionRequest updateRequest() {
        return new UpdateSubscriptionRequest(new BigDecimal("19.99"), BillingCycle.YEARLY, TODAY.plusDays(3), true);
    }
}
