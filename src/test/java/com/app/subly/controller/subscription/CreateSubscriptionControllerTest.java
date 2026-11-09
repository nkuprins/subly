package com.app.subly.controller.subscription;

import static com.app.subly.testdata.TestProviders.CATEGORY_NAME;
import static com.app.subly.testdata.TestProviders.CUSTOM_PROVIDER_NAME;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.model.subscription.BillingCycle;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class CreateSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void create_shouldReturn201WithTheSubscription() throws Exception {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest(
                PROVIDER_ID, DEFAULT_PRICE, BillingCycle.MONTHLY, DEFAULT_NEXT_PAYMENT_DATE, false);
        when(service.create(ALICE, request)).thenReturn(subscriptionResponse());

        sendJson(post(URL), VALID_CREATE_BODY)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$.providerId").value(PROVIDER_ID))
                .andExpect(jsonPath("$.providerName").value(CUSTOM_PROVIDER_NAME))
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID))
                .andExpect(jsonPath("$.categoryName").value(CATEGORY_NAME))
                .andExpect(jsonPath("$.providerType").value("CUSTOM"))
                .andExpect(jsonPath("$.price").value(35.00))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.billingCycle").value("MONTHLY"))
                .andExpect(jsonPath("$.nextPaymentDate").value(DEFAULT_NEXT_PAYMENT_DATE.toString()))
                .andExpect(jsonPath("$.freeTrial").value(false));
    }

    @Test
    void create_shouldReturn400ForUnknownProvider() throws Exception {
        ProviderNotFoundException exception = new ProviderNotFoundException(PROVIDER_ID);
        when(service.create(anyString(), any())).thenThrow(exception);

        sendJson(post(URL), VALID_CREATE_BODY).andExpect(problem(HttpStatus.BAD_REQUEST, exception));
    }
}
