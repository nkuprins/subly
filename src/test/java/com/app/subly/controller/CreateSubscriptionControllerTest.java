package com.app.subly.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.model.subscription.BillingCycle;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CreateSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void returns201() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                PROVIDER_ID, new BigDecimal("35.00"), BillingCycle.MONTHLY, NEXT_PAYMENT, false);
        given(service.create(USERNAME, request)).willReturn(gymResponse());

        send(BodyEndpoint.CREATE, BodyEndpoint.CREATE.validBody())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.providerId").value(PROVIDER_ID))
                .andExpect(jsonPath("$.providerName").value(CUSTOM_NAME))
                .andExpect(jsonPath("$.providerType").value("CUSTOM"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void returns400ForUnknownProvider() throws Exception {
        given(service.create(anyString(), any())).willThrow(new ProviderNotFoundException(99L));

        send(BodyEndpoint.CREATE, BodyEndpoint.CREATE.validBody())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Provider 99 does not exist"));
    }

    @Test
    void returns400ForPaymentDateInPast() throws Exception {
        given(service.create(anyString(), any())).willThrow(new PaymentDateInPastException(TODAY.minusDays(1)));

        send(BodyEndpoint.CREATE, BodyEndpoint.CREATE.validBody())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Next payment date " + TODAY.minusDays(1) + " is in the past"));
    }

    @Test
    void returns400ForMissingProviderId() throws Exception {
        send(BodyEndpoint.CREATE, BodyEndpoint.CREATE.bodyWithout("providerId")).andExpect(validationError());
    }
}
