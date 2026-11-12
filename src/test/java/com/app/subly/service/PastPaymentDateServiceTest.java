package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.BillingCycle;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.testdata.TestClock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PastPaymentDateServiceTest extends AbstractSubscriptionServiceTest {

    static final LocalDate YESTERDAY = TODAY.minusDays(1);

    @Test
    void create_shouldRejectYesterdayInOwnersZone() {
        stubAliceExists();
        CreateSubscriptionRequest request = createRequest(YESTERDAY);

        assertThatThrownBy(() -> service.create(ALICE, request))
                .isInstanceOf(PaymentDateInPastException.class)
                .hasMessageContaining(YESTERDAY.toString());
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void create_shouldAcceptToday() {
        stubAliceExists();
        when(subscriptionProviderRepository.getProvider(PROVIDER_ID)).thenReturn(catalogProvider());

        service.create(ALICE, createRequest(TODAY));

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void create_shouldUseOwnersTimeZone() {
        stubAliceExists();
        alice.setTimeZone(TestClock.TOKYO);
        CreateSubscriptionRequest request = createRequest(TODAY);

        assertThatThrownBy(() -> service.create(ALICE, request)).isInstanceOf(PaymentDateInPastException.class);
    }

    @Test
    void update_shouldRejectPastDateAndKeepSubscription() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnerMembership(subscription);
        LocalDate originalDate = subscription.getNextPaymentDate();
        UpdateSubscriptionRequest request =
                new UpdateSubscriptionRequest(DEFAULT_PRICE, BillingCycle.MONTHLY, YESTERDAY, false);

        assertThatThrownBy(() -> service.update(ALICE, SUBSCRIPTION_ID, request))
                .isInstanceOf(PaymentDateInPastException.class);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(originalDate);
    }
}
