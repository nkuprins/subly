package com.app.subly.controller;

import com.app.subly.dto.subscription.UpdateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exceptions.subscription.CatalogServiceReadOnlyException;
import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exceptions.subscription.SubscriptionNotFoundException;
import com.app.subly.models.subscription.BillingCycle;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UpdateSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Nested
    class Update {

        @Test
        void returns200() throws Exception {
            UpdateSubscriptionRequest request = new UpdateSubscriptionRequest(
                    new BigDecimal("35.00"), BillingCycle.MONTHLY, NEXT_PAYMENT, false);
            given(service.update(USERNAME, SUBSCRIPTION_ID, request)).willReturn(gymResponse());

            send(Endpoint.UPDATE, Endpoint.UPDATE.validBody())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID));
        }

        @Test
        void returns404ForUnknownSubscription() throws Exception {
            given(service.update(anyString(), anyLong(), any()))
                    .willThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            send(Endpoint.UPDATE, Endpoint.UPDATE.validBody())
                    .andExpect(status().isNotFound());
        }

        @Test
        void returns403ForNonOwner() throws Exception {
            given(service.update(anyString(), anyLong(), any()))
                    .willThrow(new SubscriptionAccessDeniedException(SUBSCRIPTION_ID));

            send(Endpoint.UPDATE, Endpoint.UPDATE.validBody())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value(startsWith("Access denied")));
        }
    }

    @Nested
    class UpdateCustom {

        @Test
        void returns200() throws Exception {
            UpdateCustomSubscriptionRequest request = new UpdateCustomSubscriptionRequest(
                    CUSTOM_NAME, CATEGORY_ID, new BigDecimal("35.00"), BillingCycle.MONTHLY, NEXT_PAYMENT, false);
            given(service.updateCustom(USERNAME, SUBSCRIPTION_ID, request)).willReturn(gymResponse());

            send(Endpoint.UPDATE_CUSTOM, Endpoint.UPDATE_CUSTOM.validBody())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID));
        }

        @Test
        void returns400ForCatalogSubscription() throws Exception {
            given(service.updateCustom(anyString(), anyLong(), any()))
                    .willThrow(new CatalogServiceReadOnlyException("Netflix"));

            send(Endpoint.UPDATE_CUSTOM, Endpoint.UPDATE_CUSTOM.validBody())
                    .andExpect(status().isBadRequest());
        }
    }
}
