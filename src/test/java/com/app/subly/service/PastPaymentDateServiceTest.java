package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.testdata.TestClock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PastPaymentDateServiceTest extends AbstractSubscriptionServiceTest {

    private static final BigDecimal PRICE = new BigDecimal("17.99");

    @Test
    void create_shouldRejectYesterdayInOwnersZone() {
        stubOwnerExists();
        CreateSubscriptionRequest request = createRequest(TODAY.minusDays(1));

        assertThatThrownBy(() -> subscriptionService.create(ALICE, request))
                .isInstanceOf(PaymentDateInPastException.class)
                .hasMessageContaining(TODAY.minusDays(1).toString());
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void create_shouldAcceptToday() {
        stubOwnerExists();
        when(subscriptionProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(catalogProvider()));

        subscriptionService.create(ALICE, createRequest(TODAY));

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void create_shouldUseOwnersTimeZone() {
        stubOwnerExists();
        owner.setTimeZone(TestClock.TOKYO);
        CreateSubscriptionRequest request = createRequest(TODAY);

        assertThatThrownBy(() -> subscriptionService.create(ALICE, request))
                .isInstanceOf(PaymentDateInPastException.class);
    }

    @Test
    void update_shouldRejectPastDateAndKeepSubscription() {
        Subscription subscription = subscription(catalogProvider());
        stubOwnerRowWithSubscriptionDetails(subscription);
        LocalDate originalDate = subscription.getNextPaymentDate();
        UpdateSubscriptionRequest request = updateRequest(TODAY.minusDays(1));

        assertThatThrownBy(() -> subscriptionService.update(ALICE, SUBSCRIPTION_ID, request))
                .isInstanceOf(PaymentDateInPastException.class);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(originalDate);
    }

    private static UpdateSubscriptionRequest updateRequest(LocalDate nextPaymentDate) {
        return new UpdateSubscriptionRequest(PRICE, BillingCycle.MONTHLY, nextPaymentDate, false);
    }

    private static CreateSubscriptionRequest createRequest(LocalDate nextPaymentDate) {
        return new CreateSubscriptionRequest(PROVIDER_ID, PRICE, BillingCycle.MONTHLY, nextPaymentDate, false);
    }
}
