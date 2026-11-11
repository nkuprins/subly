package com.app.subly.service;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.ProviderType;
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
}
