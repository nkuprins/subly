package com.app.subly.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.subscription.BillingCycle;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UpdateSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Nested
    class Update {

        @Test
        void returns200() throws Exception {
            UpdateSubscriptionRequest request =
                    new UpdateSubscriptionRequest(new BigDecimal("35.00"), BillingCycle.MONTHLY, NEXT_PAYMENT, false);
            given(service.update(USERNAME, SUBSCRIPTION_ID, request)).willReturn(gymResponse());

            send(BodyEndpoint.UPDATE, BodyEndpoint.UPDATE.validBody())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID));
        }

        @Test
        void returns404ForUnknownSubscription() throws Exception {
            given(service.update(anyString(), anyLong(), any()))
                    .willThrow(new SubscriptionNotFoundException(SUBSCRIPTION_ID));

            send(BodyEndpoint.UPDATE, BodyEndpoint.UPDATE.validBody())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(containsString("does not exist")));
        }

        @Test
        void returns403ForNonOwner() throws Exception {
            given(service.update(anyString(), anyLong(), any()))
                    .willThrow(new SubscriptionAccessDeniedException(SUBSCRIPTION_ID));

            send(BodyEndpoint.UPDATE, BodyEndpoint.UPDATE.validBody())
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value(startsWith("Access denied")));
        }
    }
}
