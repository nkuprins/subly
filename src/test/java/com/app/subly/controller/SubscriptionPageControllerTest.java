package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.dto.subscription.ActiveSubscriptionResponse;
import com.app.subly.dto.subscription.CancelledSubscriptionResponse;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.ProviderType;
import com.app.subly.service.ProviderService;
import com.app.subly.service.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(SubscriptionPageController.class)
@WithMockUser(username = ALICE)
class SubscriptionPageControllerTest extends AbstractWebMvcTest {

    private static final long SUBSCRIPTION_ID = 7L;
    private static final long PROVIDER_ID = 3L;
    private static final String SUBSCRIPTIONS_URL = "/subscriptions";
    private static final String NEXT_PAYMENT_DATE = "2026-12-01";
    private static final LocalDate NEXT_PAYMENT = LocalDate.parse(NEXT_PAYMENT_DATE);
    private static final SubscriptionResponse SUBSCRIPTION = new SubscriptionResponse(
            SUBSCRIPTION_ID,
            "Music",
            new BigDecimal("12.50"),
            Currency.EUR,
            BillingCycle.MONTHLY,
            NEXT_PAYMENT,
            false,
            1);

    private static final ActiveSubscriptionResponse ACTIVE_SUBSCRIPTION =
            new ActiveSubscriptionResponse(SUBSCRIPTION, MembershipRole.OWNER, new BigDecimal("12.50"));

    private static final CancelledSubscriptionResponse CANCELLED_SUBSCRIPTION =
            new CancelledSubscriptionResponse(SUBSCRIPTION, MembershipRole.OWNER, NEXT_PAYMENT, new BigDecimal("5.00"));

    private static final List<ActiveSubscriptionResponse> ACTIVE_SUBSCRIPTIONS = List.of(ACTIVE_SUBSCRIPTION);
    private static final List<CancelledSubscriptionResponse> CANCELLED_SUBSCRIPTIONS = List.of(CANCELLED_SUBSCRIPTION);
    private static final List<ProviderResponse> PROVIDERS =
            List.of(new ProviderResponse(PROVIDER_ID, "Music", null, ProviderType.CATALOG));

    @MockitoBean
    SubscriptionService subscriptionService;

    @MockitoBean
    ProviderService providerService;

    @Test
    void subscriptions_shouldReturnActiveTabByDefault() throws Exception {
        when(subscriptionService.findActive(ALICE)).thenReturn(ACTIVE_SUBSCRIPTIONS);
        when(subscriptionService.findCancelled(ALICE)).thenReturn(CANCELLED_SUBSCRIPTIONS);

        mockMvc.perform(get(SUBSCRIPTIONS_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions"))
                .andExpect(model().attribute("tab", "active"))
                .andExpect(model().attribute("subscriptions", ACTIVE_SUBSCRIPTIONS))
                .andExpect(model().attribute("activeCount", 1))
                .andExpect(model().attribute("cancelledCount", 1));

        verify(subscriptionService).findActive(ALICE);
        verify(subscriptionService).findCancelled(ALICE);
    }

    @Test
    void subscriptions_shouldReturnCancelledTabWhenRequested() throws Exception {
        when(subscriptionService.findActive(ALICE)).thenReturn(ACTIVE_SUBSCRIPTIONS);
        when(subscriptionService.findCancelled(ALICE)).thenReturn(CANCELLED_SUBSCRIPTIONS);

        mockMvc.perform(get(SUBSCRIPTIONS_URL).param("tab", "cancelled"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions"))
                .andExpect(model().attribute("tab", "cancelled"))
                .andExpect(model().attribute("subscriptions", CANCELLED_SUBSCRIPTIONS))
                .andExpect(model().attribute("activeCount", 1))
                .andExpect(model().attribute("cancelledCount", 1));
    }

    @Test
    void subscriptions_shouldReturnActiveTabWhenUnknownTabIsRequested() throws Exception {
        when(subscriptionService.findActive(ALICE)).thenReturn(ACTIVE_SUBSCRIPTIONS);
        when(subscriptionService.findCancelled(ALICE)).thenReturn(CANCELLED_SUBSCRIPTIONS);

        mockMvc.perform(get(SUBSCRIPTIONS_URL).param("tab", "unknown"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscriptions"))
                .andExpect(model().attribute("tab", "active"))
                .andExpect(model().attribute("subscriptions", ACTIVE_SUBSCRIPTIONS));
    }

    @Test
    void newSubscriptions_shouldReturnFormWithVisibleProviders() throws Exception {
        when(providerService.findVisibleProviders(ALICE)).thenReturn(PROVIDERS);

        mockMvc.perform(get(SUBSCRIPTIONS_URL + "/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription-form"))
                .andExpect(model().attribute("providers", PROVIDERS));

        verify(providerService).findVisibleProviders(ALICE);
    }

    @Test
    void editSubscriptions_shouldReturnFormWithSubscription() throws Exception {
        when(subscriptionService.getActive(ALICE, SUBSCRIPTION_ID)).thenReturn(SUBSCRIPTION);

        mockMvc.perform(get(SUBSCRIPTIONS_URL + "/{id}/edit", SUBSCRIPTION_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("subscription-form"))
                .andExpect(model().attribute("editId", SUBSCRIPTION_ID))
                .andExpect(model().attribute("sub", SUBSCRIPTION));

        verify(subscriptionService).getActive(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void createSubscription_shouldUseDefaultValuesAndRedirectWhenCreated() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS_URL)
                        .with(csrf())
                        .param("providerId", Long.toString(PROVIDER_ID))
                        .param("price", "12.50")
                        .param("billingCycle", BillingCycle.MONTHLY.name())
                        .param("nextPaymentDate", NEXT_PAYMENT_DATE))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions"));

        verify(subscriptionService)
                .create(
                        ALICE,
                        new CreateSubscriptionRequest(
                                PROVIDER_ID,
                                new BigDecimal("12.50"),
                                Currency.EUR,
                                BillingCycle.MONTHLY,
                                NEXT_PAYMENT,
                                false,
                                1));
    }

    @Test
    void cancelSubscription_shouldRedirectWithNoticeWhenCancelled() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS_URL + "/{id}/cancel", SUBSCRIPTION_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions"))
                .andExpect(flash().attribute(
                                "notice", "Subscription cancelled. You can find it under the Cancelled tab."));

        verify(subscriptionService).cancel(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void reactivateSubscription_shouldRedirectWithNoticeWhenReactivated() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS_URL + "/{id}/reactivate", SUBSCRIPTION_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions"))
                .andExpect(flash().attribute("notice", "Subscription reactivated. It's back in your active list."));

        verify(subscriptionService).reactivate(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void deleteSubscription_shouldRedirectToActiveSubscriptionsByDefault() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS_URL + "/{id}/delete", SUBSCRIPTION_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions"));

        verify(subscriptionService).delete(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void deleteSubscription_shouldRedirectToCancelledTabWhenRequested() throws Exception {
        mockMvc.perform(post(SUBSCRIPTIONS_URL + "/{id}/delete", SUBSCRIPTION_ID)
                        .with(csrf())
                        .param("tab", "cancelled"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions?tab=cancelled"));

        verify(subscriptionService).delete(ALICE, SUBSCRIPTION_ID);
    }
}
