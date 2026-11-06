package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.Subscription;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PastPaymentDateServiceTest extends AbstractSubscriptionServiceTest {

    @BeforeEach
    void ownerExists() {
        givenOwnerExists();
    }

    @Test
    void createRejectsYesterdayInOwnersZone() {
        CreateSubscriptionRequest request = catalogRequest(today.minusDays(1));

        assertThatThrownBy(() -> subscriptionService.create(OWNER, request))
                .isInstanceOf(PaymentDateInPastException.class)
                .hasMessageContaining(today.minusDays(1).toString());
        then(subscriptionRepository).shouldHaveNoInteractions();
    }

    @Test
    void createAcceptsToday() {
        given(subscriptionProviderRepository.findByIdAndProviderType(PROVIDER_ID, ProviderType.CATALOG))
                .willReturn(Optional.of(catalogProvider()));

        subscriptionService.create(OWNER, catalogRequest(today));

        then(subscriptionRepository).should().save(any(Subscription.class));
    }

    @Test
    void createUsesOwnersTimeZone() {
        owner.setTimeZone(ZoneId.of("Asia/Tokyo"));
        CreateSubscriptionRequest request = catalogRequest(today);

        assertThatThrownBy(() -> subscriptionService.create(OWNER, request))
                .isInstanceOf(PaymentDateInPastException.class);
    }

    @Test
    void updateRejectsPastDateAndKeepsSubscription() {
        Subscription subscription = givenOwnedCatalogSubscription();
        LocalDate originalDate = subscription.getNextPaymentDate();
        UpdateSubscriptionRequest request = updateRequest(today.minusDays(1));

        assertThatThrownBy(() -> subscriptionService.update(OWNER, SUBSCRIPTION_ID, request))
                .isInstanceOf(PaymentDateInPastException.class);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(originalDate);
    }

    private UpdateSubscriptionRequest updateRequest(LocalDate nextPaymentDate) {
        return new UpdateSubscriptionRequest(new BigDecimal("17.99"), BillingCycle.MONTHLY, nextPaymentDate, false);
    }

    private CreateSubscriptionRequest catalogRequest(LocalDate nextPaymentDate) {
        return new CreateSubscriptionRequest(
                PROVIDER_ID, new BigDecimal("17.99"), BillingCycle.MONTHLY, nextPaymentDate, false);
    }
}
