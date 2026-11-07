package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.exception.subscription.ProviderNotFoundException;
import com.app.subly.model.User;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionProvider;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class CreateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Captor
    ArgumentCaptor<Subscription> subscriptionCaptor;

    @Captor
    ArgumentCaptor<SubscriptionMembership> membershipCaptor;

    @BeforeEach
    void ownerExists() {
        owner.setId(1L);
        given(userRepository.findByUsername(OWNER)).willReturn(Optional.of(owner));
    }

    @Test
    void savesSubscriptionForCatalogProvider() {
        assertSubscribesTo(catalogProvider());
    }

    @Test
    void savesSubscriptionForOwnCustomProvider() {
        assertSubscribesTo(customProvider());
    }

    @Test
    void rejectsUnknownProvider() {
        given(subscriptionProviderRepository.findById(PROVIDER_ID)).willReturn(Optional.empty());

        assertRejected();
    }

    @Test
    void rejectsCustomProviderOfAnotherUser() {
        User stranger = new User();
        stranger.setId(2L);
        given(subscriptionProviderRepository.findById(PROVIDER_ID))
                .willReturn(Optional.of(SubscriptionProvider.custom(CUSTOM_NAME, gym, stranger)));

        assertRejected();
    }

    private void assertRejected() {
        CreateSubscriptionRequest request = request();

        assertThatThrownBy(() -> subscriptionService.create(OWNER, request))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining(String.valueOf(PROVIDER_ID));
        then(subscriptionRepository).shouldHaveNoInteractions();
        then(membershipRepository).shouldHaveNoInteractions();
    }

    private void assertSubscribesTo(SubscriptionProvider provider) {
        given(subscriptionProviderRepository.findById(PROVIDER_ID)).willReturn(Optional.of(provider));
        CreateSubscriptionRequest request = request();

        SubscriptionResponse response = subscriptionService.create(OWNER, request);

        then(subscriptionRepository).should().save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getValue();
        assertThat(saved.getProvider()).isSameAs(provider);
        then(subscriptionProviderRepository).should(never()).save(any());
        assertPaymentDetails(saved, request);

        then(membershipRepository).should().save(membershipCaptor.capture());
        SubscriptionMembership membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(owner);
        assertThat(membership.getSubscription()).isSameAs(saved);
        assertThat(membership.getRole()).isEqualTo(MembershipRole.OWNER);

        assertThat(response).isEqualTo(SubscriptionResponse.from(saved));
    }

    private CreateSubscriptionRequest request() {
        return new CreateSubscriptionRequest(
                PROVIDER_ID, new BigDecimal("17.99"), BillingCycle.MONTHLY, today.plusDays(10), false);
    }
}
