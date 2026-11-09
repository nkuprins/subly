package com.app.subly.controller.subscription;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class DeleteSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void delete_shouldReturn204() throws Exception {
        mockMvc.perform(delete(ITEM_URL)).andExpect(status().isNoContent());

        verify(service).delete(ALICE, SUBSCRIPTION_ID);
    }
}
