package com.app.subly.controller.subscription;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.subscription.VisibleSubscriptionResponse;
import com.app.subly.model.subscription.MembershipRole;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void listMine_shouldReturn200WithOwnedSubscriptions() throws Exception {
        when(service.findVisibleTo(ALICE))
                .thenReturn(List.of(new VisibleSubscriptionResponse(subscriptionResponse(), MembershipRole.OWNER)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscription.id").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }
}
