package com.app.subly.controller;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class SubscriptionLifecycleControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void cancelReturns204() throws Exception {
        mockMvc.perform(patch(URL + "/" + SUBSCRIPTION_ID + "/cancel")).andExpect(status().isNoContent());

        then(service).should().cancel(USERNAME, SUBSCRIPTION_ID);
    }

    @Test
    void reactivateReturns204() throws Exception {
        mockMvc.perform(patch(URL + "/" + SUBSCRIPTION_ID + "/reactivate")).andExpect(status().isNoContent());

        then(service).should().reactivate(USERNAME, SUBSCRIPTION_ID);
    }
}
