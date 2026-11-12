package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.SUBSCRIPTION_ID;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.ALICE_ID;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.ProviderType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DeleteSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @ParameterizedTest
    @EnumSource(ProviderType.class)
    void delete_shouldDeleteSubscriptionAndKeepProvider(ProviderType providerType) {
        Subscription subscription = subscription(provider(providerType));
        stubOwnedSubscription(subscription);

        service.delete(ALICE, SUBSCRIPTION_ID);

        verify(subscriptionRepository).delete(subscription);
        verifyNoInteractions(subscriptionProviderRepository);
    }

    @Test
    void deleteAllSubscriptionsFor_shouldDeleteMembershipsAndOwnedSubscriptions() {
        Subscription owned = aSubscription().withId(SUBSCRIPTION_ID).build();
        when(membershipRepository.findByUserIdAndRole(ALICE_ID, MembershipRole.OWNER))
                .thenReturn(List.of(SubscriptionMembership.owner(alice, owned)));

        service.deleteAllSubscriptionsFor(ALICE_ID);

        verify(membershipRepository).deleteByUserId(ALICE_ID);
        verify(subscriptionRepository).deleteAllById(List.of(SUBSCRIPTION_ID));
    }

    @Test
    void deleteAllSubscriptionsFor_shouldOnlyDeleteMembershipsWhenUserOwnsNothing() {
        when(membershipRepository.findByUserIdAndRole(ALICE_ID, MembershipRole.OWNER))
                .thenReturn(List.of());

        service.deleteAllSubscriptionsFor(ALICE_ID);

        verify(membershipRepository).deleteByUserId(ALICE_ID);
        verifyNoInteractions(subscriptionRepository);
    }
}
