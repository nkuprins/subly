package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.ProviderType;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.testdata.TestClock;
import com.app.subly.testdata.TestProviders;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class CreateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    private static final long PROVIDER_ID = 3L;

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
        when(subscriptionProviderRepository.getProvider(PROVIDER_ID)).thenReturn(provider);

        service.create(ALICE, createRequest(DEFAULT_NEXT_PAYMENT_DATE));

        verify(subscriptionRepository).save(subscriptionCaptor.capture());
        Subscription saved = subscriptionCaptor.getValue();
        assertThat(saved.getProvider()).isSameAs(provider);
        assertThat(saved.getCurrency()).isEqualTo(Currency.USD);
        verify(subscriptionProviderRepository, never()).save(any());
        assertPaymentDetails(saved, DEFAULT_PRICE, BillingCycle.MONTHLY, DEFAULT_NEXT_PAYMENT_DATE, true);

        verify(membershipRepository).save(membershipCaptor.capture());
        SubscriptionMembership membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(alice);
        assertThat(membership.getSubscription()).isSameAs(saved);
        assertThat(membership.getRole()).isEqualTo(MembershipRole.OWNER);
    }

    @Test
    void create_shouldAcceptToday() {
        when(subscriptionProviderRepository.getProvider(PROVIDER_ID)).thenReturn(catalogProvider());

        service.create(ALICE, createRequest(TODAY));

        verify(subscriptionRepository).save(any(Subscription.class));
    }

    @Test
    void create_shouldRejectPastDateInOwnersTimeZone() {
        // 16:00 UTC on 4 Nov is already 5 Nov in Tokyo
        alice.setTimeZone(TestClock.TOKYO);
        CreateSubscriptionRequest request = createRequest(TODAY);

        assertThatThrownBy(() -> service.create(ALICE, request))
                .isInstanceOf(PaymentDateInPastException.class)
                .hasMessageContaining(TODAY.toString());
        verifyNoInteractions(subscriptionRepository);
    }

    @Test
    void create_shouldRejectCustomProviderOfAnotherUser() {
        when(subscriptionProviderRepository.getProvider(PROVIDER_ID))
                .thenReturn(TestProviders.custom(TestProviders.category(), bob));
        CreateSubscriptionRequest request = createRequest(DEFAULT_NEXT_PAYMENT_DATE);

        assertThatThrownBy(() -> service.create(ALICE, request))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining(String.valueOf(PROVIDER_ID));
        verifyNoInteractions(subscriptionRepository, membershipRepository);
    }

    private static CreateSubscriptionRequest createRequest(LocalDate nextPaymentDate) {
        return new CreateSubscriptionRequest(
                PROVIDER_ID, DEFAULT_PRICE, Currency.USD, BillingCycle.MONTHLY, nextPaymentDate, true, 1);
    }
}
