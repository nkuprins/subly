package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

import com.app.subly.dto.subscription.CreateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.exception.category.InvalidCategoryException;
import com.app.subly.exception.subscription.CatalogProviderNotFoundException;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionProvider;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

class CreateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Captor
    ArgumentCaptor<SubscriptionProvider> providerCaptor;

    @Captor
    ArgumentCaptor<Subscription> subscriptionCaptor;

    @Captor
    ArgumentCaptor<SubscriptionMembership> membershipCaptor;

    @BeforeEach
    void ownerExists() {
        given(userRepository.findByUsername(OWNER)).willReturn(Optional.of(owner));
    }

    @Nested
    class Create {

        @Test
        void savesSubscriptionForExistingCatalogProvider() {
            SubscriptionProvider catalogProvider = catalogProvider();
            given(subscriptionProviderRepository.findByIdAndProviderType(PROVIDER_ID, ProviderType.CATALOG))
                    .willReturn(Optional.of(catalogProvider));

            CreateSubscriptionRequest request = catalogRequest();

            SubscriptionResponse response = subscriptionService.create(OWNER, request);

            then(subscriptionRepository).should().save(subscriptionCaptor.capture());
            Subscription saved = subscriptionCaptor.getValue();
            assertThat(saved.getProvider()).isSameAs(catalogProvider);
            then(subscriptionProviderRepository).should(never()).save(any());
            assertPaymentDetails(saved, request);
            assertSavedOwnerMembership(saved);
            assertThat(response).isEqualTo(SubscriptionResponse.from(saved));
        }

        @Test
        void rejectsProviderThatIsNotInTheCatalog() {
            given(subscriptionProviderRepository.findByIdAndProviderType(PROVIDER_ID, ProviderType.CATALOG))
                    .willReturn(Optional.empty());
            CreateSubscriptionRequest request = catalogRequest();

            assertThatThrownBy(() -> subscriptionService.create(OWNER, request))
                    .isInstanceOf(CatalogProviderNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PROVIDER_ID));
            then(subscriptionRepository).shouldHaveNoInteractions();
            then(membershipRepository).shouldHaveNoInteractions();
        }

        private CreateSubscriptionRequest catalogRequest() {
            return new CreateSubscriptionRequest(
                    PROVIDER_ID, new BigDecimal("17.99"), BillingCycle.MONTHLY, today.plusDays(10), false);
        }
    }

    @Nested
    class CreateCustom {

        @Test
        void savesSubscriptionForNewCustomProviderWithStrippedName() {
            given(categoryRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(subscriptionProviderRepository.save(any(SubscriptionProvider.class)))
                    .willAnswer(returnsFirstArg());
            CreateCustomSubscriptionRequest request = gymRequest();

            SubscriptionResponse response = subscriptionService.createCustom(OWNER, request);

            then(subscriptionProviderRepository).should().save(providerCaptor.capture());
            SubscriptionProvider savedProvider = providerCaptor.getValue();
            assertThat(savedProvider.getName()).isEqualTo(CUSTOM_NAME);
            assertThat(savedProvider.getProviderType()).isEqualTo(ProviderType.CUSTOM);
            assertThat(savedProvider.getCategory()).isSameAs(gym);
            assertThat(savedProvider.getCreatedBy()).isSameAs(owner);

            then(subscriptionRepository).should().save(subscriptionCaptor.capture());
            Subscription saved = subscriptionCaptor.getValue();
            assertThat(saved.getProvider()).isSameAs(savedProvider);
            assertPaymentDetails(saved, request);
            assertSavedOwnerMembership(saved);

            assertThat(response).isEqualTo(SubscriptionResponse.from(saved));
        }

        @Test
        void rejectsUnknownCategory() {
            given(categoryRepository.findById(GYM_ID)).willReturn(Optional.empty());
            CreateCustomSubscriptionRequest request = gymRequest();

            assertThatThrownBy(() -> subscriptionService.createCustom(OWNER, request))
                    .isInstanceOf(InvalidCategoryException.class)
                    .hasMessageContaining(String.valueOf(GYM_ID));
            then(subscriptionProviderRepository).shouldHaveNoInteractions();
            then(subscriptionRepository).shouldHaveNoInteractions();
            then(membershipRepository).shouldHaveNoInteractions();
        }

        private CreateCustomSubscriptionRequest gymRequest() {
            return new CreateCustomSubscriptionRequest(
                    "  " + CUSTOM_NAME + "  ",
                    GYM_ID,
                    new BigDecimal("35.50"),
                    BillingCycle.MONTHLY,
                    today.plusDays(10),
                    true);
        }
    }

    private void assertSavedOwnerMembership(Subscription subscription) {
        then(membershipRepository).should().save(membershipCaptor.capture());
        SubscriptionMembership membership = membershipCaptor.getValue();
        assertThat(membership.getUser()).isSameAs(owner);
        assertThat(membership.getSubscription()).isSameAs(subscription);
        assertThat(membership.getRole()).isEqualTo(MembershipRole.OWNER);
    }
}
