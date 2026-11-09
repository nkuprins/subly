package com.app.subly.service;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeleteSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @ParameterizedTest
    @EnumSource(ProviderType.class)
    void delete_shouldDeleteSubscriptionAndKeepProvider(ProviderType providerType) {
        Subscription subscription = stubOwnedSubscription(providerType);

        subscriptionService.delete(ALICE, SUBSCRIPTION_ID);

        verify(subscriptionRepository).delete(subscription);
        verifyNoInteractions(subscriptionProviderRepository);
    }

    @Test
    void delete_shouldRejectNonOwner() {
        stubSubscriptionOwnedBySomeoneElse();

        assertThatThrownBy(() -> subscriptionService.delete(BOB, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionAccessDeniedException.class);
        verify(subscriptionRepository, never()).delete(any());
        verifyNoInteractions(subscriptionProviderRepository);
    }

    @Test
    void delete_shouldRejectUnknownSubscription() {
        when(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.delete(ALICE, SUBSCRIPTION_ID))
                .isInstanceOf(SubscriptionNotFoundException.class);
        verify(subscriptionRepository, never()).delete(any());
        verifyNoInteractions(subscriptionProviderRepository);
    }
}
