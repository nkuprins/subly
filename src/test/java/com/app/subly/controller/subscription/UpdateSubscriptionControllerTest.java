package com.app.subly.controller.subscription;

import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.testdata.TestClock;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class UpdateSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void update_shouldReturn200WithTheSubscription() throws Exception {
        UpdateSubscriptionRequest request =
                new UpdateSubscriptionRequest(DEFAULT_PRICE, BillingCycle.MONTHLY, DEFAULT_NEXT_PAYMENT_DATE, false);
        when(service.update(ALICE, SUBSCRIPTION_ID, request)).thenReturn(subscriptionResponse());

        sendJson(put(ITEM_URL), VALID_UPDATE_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(SUBSCRIPTION_ID));
    }

    @Test
    void update_shouldReturn400ForPaymentDateInPast() throws Exception {
        LocalDate yesterday = TestClock.TODAY.minusDays(1);
        PaymentDateInPastException exception = new PaymentDateInPastException(yesterday);
        when(service.update(anyString(), anyLong(), any())).thenThrow(exception);

        sendJson(put(ITEM_URL), VALID_UPDATE_BODY.with("nextPaymentDate", yesterday))
                .andExpect(problem(HttpStatus.BAD_REQUEST, exception));
    }

    @Test
    void update_shouldReturn404ForUnknownSubscription() throws Exception {
        SubscriptionNotFoundException exception = new SubscriptionNotFoundException(SUBSCRIPTION_ID);
        when(service.update(anyString(), anyLong(), any())).thenThrow(exception);

        sendJson(put(ITEM_URL), VALID_UPDATE_BODY).andExpect(problem(HttpStatus.NOT_FOUND, exception));
    }

    @Test
    void update_shouldReturn403ForNonOwner() throws Exception {
        SubscriptionAccessDeniedException exception = new SubscriptionAccessDeniedException(SUBSCRIPTION_ID);
        when(service.update(anyString(), anyLong(), any())).thenThrow(exception);

        sendJson(put(ITEM_URL), VALID_UPDATE_BODY).andExpect(problem(HttpStatus.FORBIDDEN, exception));
    }
}
