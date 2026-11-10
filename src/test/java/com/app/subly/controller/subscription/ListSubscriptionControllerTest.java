package com.app.subly.controller.subscription;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.subscription.CancelledSubscriptionResponse;
import com.app.subly.dto.subscription.VisibleSubscriptionResponse;
import com.app.subly.model.subscription.MembershipRole;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ListSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    static final String CANCELLED_URL = URL + "/cancelled";

    @Test
    void listMine_shouldReturn200WithOwnedSubscriptions() throws Exception {
        when(service.findCurrent(ALICE))
                .thenReturn(List.of(new VisibleSubscriptionResponse(subscriptionResponse(), MembershipRole.OWNER)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscription.id").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void listCancelled_shouldReturn200WithCancelledSubscriptions() throws Exception {
        LocalDate endsOn = LocalDate.of(2026, 10, 4);
        when(service.findCancelled(ALICE))
                .thenReturn(List.of(new CancelledSubscriptionResponse(
                        subscriptionResponse(), MembershipRole.MEMBER, endsOn, new BigDecimal("10.00"))));

        mockMvc.perform(get(CANCELLED_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscription.id").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$[0].role").value("MEMBER"))
                .andExpect(jsonPath("$[0].endsOn").value(endsOn.toString()))
                .andExpect(jsonPath("$[0].saved").value(10.00));
    }
}
