package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.user.User;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
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
        stubOwnerExists();
    }

    @Test
    void create_shouldSaveSubscriptionForCatalogProvider() {
        assertSubscribesTo(catalogProvider());
    }

    @Test
    void create_shouldSaveSubscriptionForOwnCustomProvider() {
        assertSubscribesTo(customProvider());
    }

    @Test
    void create_shouldRejectUnknownProvider() {
        when(subscriptionProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.empty());

        assertRejected();
    }

    @Test
    void create_shouldRejectCustomProviderOfAnotherUser() {
        User stranger = TestUsers.user(BOB);
        stranger.setId(2L);
        when(subscriptionProviderRepository.findById(PROVIDER_ID))
                .thenReturn(Optional.of(TestProviders.custom(category, stranger)));

        assertRejected();
    }

    private void assertRejected() {
        CreateSubscriptionRequest request = request();

        assertThatThrownBy(() -> subscriptionService.create(ALICE, request))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining(String.valueOf(PROVIDER_ID));
        verifyNoInteractions(subscriptionRepository, membershipRepository);
    }

    private void assertSubscribesTo(SubscriptionProvider provider) {
        when(subscriptionProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(provider));
        CreateSubscriptionRequest request = request();

        SubscriptionResponse response = subscriptionService.create(ALICE, request);

        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getValue();
        assertThat(saved.getProvider()).isSameAs(provider);
        verify(subscriptionProviderRepository, never()).save(any());
        assertPaymentDetails(saved, request);

        verify(membershipRepository).save(membershipCaptor.capture());
        SubscriptionMembership membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(owner);
        assertThat(membership.getSubscription()).isSameAs(saved);
        assertThat(membership.getRole()).isEqualTo(MembershipRole.OWNER);

        assertThat(response).isEqualTo(SubscriptionResponse.from(saved));
    }

    private static CreateSubscriptionRequest request() {
        return new CreateSubscriptionRequest(
                PROVIDER_ID, new BigDecimal("17.99"), BillingCycle.MONTHLY, TODAY.plusDays(10), false);
    }
}
