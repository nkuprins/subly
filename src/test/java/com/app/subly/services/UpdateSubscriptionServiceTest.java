package com.app.subly.services;

import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.dto.subscription.UpdateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exceptions.subscription.CatalogServiceReadOnlyException;
import com.app.subly.exceptions.subscription.CategoryNotFoundException;
import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exceptions.subscription.SubscriptionNotFoundException;
import com.app.subly.models.subscription.BillingCycle;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.Subscription;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

class UpdateSubscriptionServiceTest extends AbstractSubscriptionServiceTest {

    static final long MUSIC_ID = 5L;

    final Category music = new Category("Test music");

    @Nested
    class Update {

        @Test
        void changesPaymentDetailsAndKeepsTheService() {
            Subscription subscription = givenOwnedCatalogSubscription();
            givenOwnerExists();
            UpdateSubscriptionRequest request = updateRequest();

            SubscriptionResponse response = subscriptionService.update(OWNER, SUBSCRIPTION_ID, request);

            assertPaymentDetails(subscription, request);
            assertThat(subscription.getService().getName()).isEqualTo(CATALOG_NAME);
            assertThat(response).isEqualTo(SubscriptionResponse.from(subscription));
        }

        @Test
        void rejectsNonOwner() {
            Subscription subscription = givenSubscriptionOwnedBySomeoneElse();
            UpdateSubscriptionRequest request = updateRequest();

            assertThatThrownBy(() -> subscriptionService.update(STRANGER, SUBSCRIPTION_ID, request))
                    .isInstanceOf(SubscriptionAccessDeniedException.class);
            assertUnchanged(subscription, CUSTOM_NAME, gym);
        }

        @Test
        void rejectsUnknownSubscription() {
            given(subscriptionRepository.findWithServiceById(SUBSCRIPTION_ID)).willReturn(Optional.empty());
            UpdateSubscriptionRequest request = updateRequest();

            assertThatThrownBy(() -> subscriptionService.update(OWNER, SUBSCRIPTION_ID, request))
                    .isInstanceOf(SubscriptionNotFoundException.class);
        }

        private UpdateSubscriptionRequest updateRequest() {
            return new UpdateSubscriptionRequest(new BigDecimal("19.99"), BillingCycle.YEARLY,
                    today.plusDays(3), true);
        }
    }

    @Nested
    class UpdateCustom {

        @Test
        void changesStrippedNameCategoryAndPaymentDetails() {
            Subscription subscription = givenOwnedCustomSubscription();
            givenOwnerExists();
            given(categoryRepository.findById(MUSIC_ID)).willReturn(Optional.of(music));
            UpdateCustomSubscriptionRequest request = updateCustomRequest();

            SubscriptionResponse response = subscriptionService.updateCustom(OWNER, SUBSCRIPTION_ID, request);

            assertThat(subscription.getService().getName()).isEqualTo("Renamed");
            assertThat(subscription.getService().getCategory()).isSameAs(music);
            assertPaymentDetails(subscription, request);
            assertThat(response).isEqualTo(SubscriptionResponse.from(subscription));
        }

        @Test
        void rejectsCatalogSubscription() {
            Subscription subscription = givenOwnedCatalogSubscription();
            UpdateCustomSubscriptionRequest request = updateCustomRequest();

            assertThatThrownBy(() -> subscriptionService.updateCustom(OWNER, SUBSCRIPTION_ID, request))
                    .isInstanceOf(CatalogServiceReadOnlyException.class);
            assertUnchanged(subscription, CATALOG_NAME, gym);
            then(categoryRepository).shouldHaveNoInteractions();
        }

        @Test
        void rejectsUnknownCategory() {
            Subscription subscription = givenOwnedCustomSubscription();
            givenOwnerExists();
            given(categoryRepository.findById(MUSIC_ID)).willReturn(Optional.empty());
            UpdateCustomSubscriptionRequest request = updateCustomRequest();

            assertThatThrownBy(() -> subscriptionService.updateCustom(OWNER, SUBSCRIPTION_ID, request))
                    .isInstanceOf(CategoryNotFoundException.class);
            assertUnchanged(subscription, CUSTOM_NAME, gym);
        }

        @Test
        void rejectsNonOwner() {
            Subscription subscription = givenSubscriptionOwnedBySomeoneElse();
            UpdateCustomSubscriptionRequest request = updateCustomRequest();

            assertThatThrownBy(() -> subscriptionService.updateCustom(STRANGER, SUBSCRIPTION_ID, request))
                    .isInstanceOf(SubscriptionAccessDeniedException.class);
            assertUnchanged(subscription, CUSTOM_NAME, gym);
            then(categoryRepository).shouldHaveNoInteractions();
        }

        @Test
        void rejectsUnknownSubscription() {
            given(subscriptionRepository.findWithServiceById(SUBSCRIPTION_ID)).willReturn(Optional.empty());
            UpdateCustomSubscriptionRequest request = updateCustomRequest();

            assertThatThrownBy(() -> subscriptionService.updateCustom(OWNER, SUBSCRIPTION_ID, request))
                    .isInstanceOf(SubscriptionNotFoundException.class);
            then(categoryRepository).shouldHaveNoInteractions();
        }

        private UpdateCustomSubscriptionRequest updateCustomRequest() {
            return new UpdateCustomSubscriptionRequest("  Renamed  ", MUSIC_ID, new BigDecimal("12.34"),
                    BillingCycle.YEARLY, today.plusDays(30), true);
        }
    }

    private static void assertUnchanged(Subscription subscription, String serviceName, Category category) {
        assertThat(subscription.getService().getName()).isEqualTo(serviceName);
        assertThat(subscription.getService().getCategory()).isSameAs(category);
        assertThat(subscription.getPrice()).isEqualByComparingTo(ORIGINAL_PRICE);
    }
}
