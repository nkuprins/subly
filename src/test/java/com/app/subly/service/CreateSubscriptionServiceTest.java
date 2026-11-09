package com.app.subly.service;

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
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.user.User;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class CreateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    static final long BOB_ID = 2L;

    @Captor
    ArgumentCaptor<Subscription> subscriptionCaptor;

    @Captor
    ArgumentCaptor<SubscriptionMembership> membershipCaptor;

    @BeforeEach
    void aliceExists() {
        stubAliceExists();
    }

    @ParameterizedTest
    @EnumSource(ProviderType.class)
    void create_shouldSaveSubscriptionWithOwnerMembership(ProviderType providerType) {
        SubscriptionProvider provider = provider(providerType);
        when(subscriptionProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.of(provider));
        CreateSubscriptionRequest request = createRequest();

        SubscriptionResponse response = service.create(ALICE, request);

        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getValue();
        assertThat(saved.getProvider()).isSameAs(provider);
        verify(subscriptionProviderRepository, never()).save(any());
        assertPaymentDetails(saved, request);

        verify(membershipRepository).save(membershipCaptor.capture());
        SubscriptionMembership membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(alice);
        assertThat(membership.getSubscription()).isSameAs(saved);
        assertThat(membership.getRole()).isEqualTo(MembershipRole.OWNER);

        assertThat(response).isEqualTo(SubscriptionResponse.from(saved));
    }

    @Test
    void create_shouldRejectUnknownProvider() {
        when(subscriptionProviderRepository.findById(PROVIDER_ID)).thenReturn(Optional.empty());

        assertRejected();
    }

    @Test
    void create_shouldRejectCustomProviderOfAnotherUser() {
        User bob = TestUsers.userWithId(BOB, BOB_ID);
        when(subscriptionProviderRepository.findById(PROVIDER_ID))
                .thenReturn(Optional.of(TestProviders.custom(TestProviders.category(), bob)));

        assertRejected();
    }

    private void assertRejected() {
        CreateSubscriptionRequest request = createRequest();

        assertThatThrownBy(() -> service.create(ALICE, request))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining(String.valueOf(PROVIDER_ID));
        verifyNoInteractions(subscriptionRepository, membershipRepository);
    }
}
