package com.app.subly.controller;

import com.app.subly.dto.subscription.CreateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.exception.subscription.CatalogServiceNotFoundException;
import com.app.subly.exception.subscription.CategoryNotFoundException;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.model.subscription.BillingCycle;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CreateSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Nested
    class Create {

        @Test
        void returns201() throws Exception {
            CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                    SERVICE_ID, new BigDecimal("35.00"), BillingCycle.MONTHLY, NEXT_PAYMENT, false);
            given(service.create(USERNAME, request)).willReturn(gymResponse());

            send(BodyEndpoint.CREATE, BodyEndpoint.CREATE.validBody())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID));
        }

        @Test
        void returns400ForUnknownService() throws Exception {
            given(service.create(anyString(), any())).willThrow(new CatalogServiceNotFoundException(99L));

            send(BodyEndpoint.CREATE, BodyEndpoint.CREATE.validBody())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Catalog service 99 does not exist"));
        }

        @Test
        void returns400ForPaymentDateInPast() throws Exception {
            given(service.create(anyString(), any())).willThrow(new PaymentDateInPastException(TODAY.minusDays(1)));

            send(BodyEndpoint.CREATE, BodyEndpoint.CREATE.validBody())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Next payment date " + TODAY.minusDays(1) + " is in the past"));
        }
    }

    @Nested
    class CreateCustom {

        @Test
        void returns201() throws Exception {
            CreateCustomSubscriptionRequest request = new CreateCustomSubscriptionRequest(
                    CUSTOM_NAME, CATEGORY_ID, new BigDecimal("35.00"), BillingCycle.MONTHLY, NEXT_PAYMENT, false);
            given(service.createCustom(USERNAME, request)).willReturn(gymResponse());

            send(BodyEndpoint.CREATE_CUSTOM, BodyEndpoint.CREATE_CUSTOM.validBody())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                    .andExpect(jsonPath("$.serviceName").value(CUSTOM_NAME))
                    .andExpect(jsonPath("$.serviceType").value("CUSTOM"))
                    .andExpect(jsonPath("$.currency").value("EUR"));
        }

        @Test
        void returns400ForUnknownCategory() throws Exception {
            given(service.createCustom(anyString(), any())).willThrow(new CategoryNotFoundException(99L));

            send(BodyEndpoint.CREATE_CUSTOM, BodyEndpoint.CREATE_CUSTOM.validBody())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value("Category 99 does not exist"));
        }
    }
}
