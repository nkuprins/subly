package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.BillingCycle;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    private static final BigDecimal NEW_PRICE = new BigDecimal("19.99");
    private static final LocalDate NEW_PAYMENT_DATE = TODAY.plusDays(3);

    @Test
    void update_shouldChangePaymentDetailsAndKeepProvider() {
        SubscriptionProvider provider = catalogProvider();
        Subscription subscription = subscription(provider);
        stubOwnerMembership(subscription);

        service.update(ALICE, SUBSCRIPTION_ID, updateRequest(NEW_PAYMENT_DATE));

        assertPaymentDetails(subscription, NEW_PRICE, BillingCycle.YEARLY, NEW_PAYMENT_DATE, true);
        assertThat(subscription.getProvider()).isSameAs(provider);
    }

    @Test
    void update_shouldRejectPastDateAndKeepSubscription() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnerMembership(subscription);
        UpdateSubscriptionRequest request = updateRequest(TODAY.minusDays(1));

        assertThatThrownBy(() -> service.update(ALICE, SUBSCRIPTION_ID, request))
                .isInstanceOf(PaymentDateInPastException.class);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(DEFAULT_NEXT_PAYMENT_DATE);
    }

    private static UpdateSubscriptionRequest updateRequest(LocalDate nextPaymentDate) {
        return new UpdateSubscriptionRequest(NEW_PRICE, BillingCycle.YEARLY, nextPaymentDate, true);
    }
}
