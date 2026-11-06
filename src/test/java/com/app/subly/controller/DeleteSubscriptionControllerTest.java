package com.app.subly.controller;

import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class DeleteSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void returns204() throws Exception {
        mockMvc.perform(delete(URL + "/" + SUBSCRIPTION_ID)).andExpect(status().isNoContent());

        then(service).should().delete(USERNAME, SUBSCRIPTION_ID);
    }
}
