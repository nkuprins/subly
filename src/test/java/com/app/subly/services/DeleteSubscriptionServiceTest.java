package com.app.subly.services;

import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exceptions.subscription.SubscriptionNotFoundException;
import com.app.subly.models.subscription.Subscription;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class DeleteSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void deletesSubscriptionAndItsCustomProvider() {
        Subscription subscription = givenOwnedCustomSubscription();

        subscriptionService.delete(OWNER, SUBSCRIPTION_ID);

        then(subscriptionRepository).should().delete(subscription);
        then(providerRepository).should().delete(subscription.getProvider());
    }

    @Test
    void deletesSubscriptionButKeepsCatalogProvider() {
        Subscription subscription = givenOwnedCatalogSubscription();

        subscriptionService.delete(OWNER, SUBSCRIPTION_ID);

        then(subscriptionRepository).should().delete(subscription);
        then(providerRepository).shouldHaveNoInteractions();
    }

    @Test
    void rejectsNonOwner() {
        givenSubscriptionOwnedBySomeoneElse();

        assertThatThrownBy(() -> subscriptionService.delete(STRANGER, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);
        then(subscriptionRepository).should(never()).delete(any());
        then(providerRepository).shouldHaveNoInteractions();
    }

    @Test
    void rejectsUnknownSubscription() {
        given(subscriptionRepository.findWithProviderById(SUBSCRIPTION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.delete(OWNER, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotFoundException.class);
        then(subscriptionRepository).should(never()).delete(any());
        then(providerRepository).shouldHaveNoInteractions();
    }
}
