package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DeleteSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Test
    void deletesSubscriptionAndItsCustomProvider() {
        Subscription subscription = givenOwnedCustomSubscription();

        subscriptionService.delete(OWNER, SUBSCRIPTION_ID);

        then(subscriptionRepository).should().delete(subscription);
        then(subscriptionProviderRepository).should().delete(subscription.getProvider());
    }

    @Test
    void deletesSubscriptionButKeepsCatalogProvider() {
        Subscription subscription = givenOwnedCatalogSubscription();

        subscriptionService.delete(OWNER, SUBSCRIPTION_ID);

        then(subscriptionRepository).should().delete(subscription);
        then(subscriptionProviderRepository).shouldHaveNoInteractions();
    }

    @Test
    void rejectsNonOwner() {
        givenSubscriptionOwnedBySomeoneElse();

        assertThatThrownBy(() -> subscriptionService.delete(STRANGER, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);
        then(subscriptionRepository).should(never()).delete(any());
        then(subscriptionProviderRepository).shouldHaveNoInteractions();
    }

    @Test
    void rejectsUnknownSubscription() {
        given(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.delete(OWNER, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotFoundException.class);
        then(subscriptionRepository).should(never()).delete(any());
        then(subscriptionProviderRepository).shouldHaveNoInteractions();
    }
}
