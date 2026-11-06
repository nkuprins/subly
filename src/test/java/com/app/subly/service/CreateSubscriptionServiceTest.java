package com.app.subly.service;

import com.app.subly.dto.subscription.CreateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.exception.subscription.CatalogServiceNotFoundException;
import com.app.subly.exception.subscription.CategoryNotFoundException;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.ServiceProvider;
import com.app.subly.model.subscription.ServiceType;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

class CreateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    @Captor ArgumentCaptor<ServiceProvider> serviceCaptor;
    @Captor ArgumentCaptor<Subscription> subscriptionCaptor;
    @Captor ArgumentCaptor<SubscriptionMembership> membershipCaptor;

    @BeforeEach
    void ownerExists() {
        given(userRepository.findByUsername(OWNER)).willReturn(Optional.of(owner));
    }

    @Nested
    class Create {

        @Test
        void savesSubscriptionForExistingCatalogService() {
            ServiceProvider catalogService = catalogService();
            given(serviceProviderRepository.findByIdAndServiceType(SERVICE_ID, ServiceType.CATALOG))
                    .willReturn(Optional.of(catalogService));

            CreateSubscriptionRequest request = catalogRequest();

            SubscriptionResponse response = subscriptionService.create(OWNER, request);

            then(subscriptionRepository).should().save(subscriptionCaptor.capture());
            Subscription saved = subscriptionCaptor.getValue();
            assertThat(saved.getService()).isSameAs(catalogService);
            then(serviceProviderRepository).should(never()).save(any());
            assertPaymentDetails(saved, request);
            assertSavedOwnerMembership(saved);
            assertThat(response).isEqualTo(SubscriptionResponse.from(saved));
        }

        @Test
        void rejectsServiceThatIsNotInTheCatalog() {
            given(serviceProviderRepository.findByIdAndServiceType(SERVICE_ID, ServiceType.CATALOG))
                    .willReturn(Optional.empty());
            CreateSubscriptionRequest request = catalogRequest();

            assertThatThrownBy(() -> subscriptionService.create(OWNER, request))
                    .isInstanceOf(CatalogServiceNotFoundException.class)
                    .hasMessageContaining(String.valueOf(SERVICE_ID));
            then(subscriptionRepository).shouldHaveNoInteractions();
            then(membershipRepository).shouldHaveNoInteractions();
        }

        private CreateSubscriptionRequest catalogRequest() {
            return new CreateSubscriptionRequest(SERVICE_ID, new BigDecimal("17.99"),
                    BillingCycle.MONTHLY, today.plusDays(10), false);
        }
    }

    @Nested
    class CreateCustom {

        @Test
        void savesSubscriptionForNewCustomServiceWithStrippedName() {
            given(categoryRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(serviceProviderRepository.save(any(ServiceProvider.class))).willAnswer(returnsFirstArg());
            CreateCustomSubscriptionRequest request = gymRequest();

            SubscriptionResponse response = subscriptionService.createCustom(OWNER, request);

            then(serviceProviderRepository).should().save(serviceCaptor.capture());
            ServiceProvider savedService = serviceCaptor.getValue();
            assertThat(savedService.getName()).isEqualTo(CUSTOM_NAME);
            assertThat(savedService.getServiceType()).isEqualTo(ServiceType.CUSTOM);
            assertThat(savedService.getCategory()).isSameAs(gym);
            assertThat(savedService.getCreatedBy()).isSameAs(owner);

            then(subscriptionRepository).should().save(subscriptionCaptor.capture());
            Subscription saved = subscriptionCaptor.getValue();
            assertThat(saved.getService()).isSameAs(savedService);
            assertPaymentDetails(saved, request);
            assertSavedOwnerMembership(saved);

            assertThat(response).isEqualTo(SubscriptionResponse.from(saved));
        }

        @Test
        void rejectsUnknownCategory() {
            given(categoryRepository.findById(GYM_ID)).willReturn(Optional.empty());
            CreateCustomSubscriptionRequest request = gymRequest();

            assertThatThrownBy(() -> subscriptionService.createCustom(OWNER, request))
                    .isInstanceOf(CategoryNotFoundException.class)
                    .hasMessageContaining(String.valueOf(GYM_ID));
            then(serviceProviderRepository).shouldHaveNoInteractions();
            then(subscriptionRepository).shouldHaveNoInteractions();
            then(membershipRepository).shouldHaveNoInteractions();
        }

        private CreateCustomSubscriptionRequest gymRequest() {
            return new CreateCustomSubscriptionRequest("  " + CUSTOM_NAME + "  ", GYM_ID,
                    new BigDecimal("35.50"), BillingCycle.MONTHLY, today.plusDays(10), true);
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
