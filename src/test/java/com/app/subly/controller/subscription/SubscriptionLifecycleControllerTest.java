package com.app.subly.controller.subscription;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class SubscriptionLifecycleControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void cancel_shouldReturn204() throws Exception {
        mockMvc.perform(patch(ITEM_URL + "/cancel")).andExpect(status().isNoContent());

        verify(service).cancel(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void reactivate_shouldReturn204() throws Exception {
        mockMvc.perform(patch(ITEM_URL + "/reactivate")).andExpect(status().isNoContent());

        verify(service).reactivate(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void cancel_shouldRejectNonPositiveId() throws Exception {
        mockMvc.perform(patch(URL + "/0/cancel"))
                .andExpect(validationError())
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].message").value("must be greater than 0"));

        verifyNoInteractions(service);
    }
}
