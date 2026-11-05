package com.app.subly.controller;

import com.app.subly.dto.subscription.VisibleSubscriptionResponse;
import com.app.subly.models.subscription.MembershipRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.test.context.support.WithAnonymousUser;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ListSubscriptionControllerTest extends AbstractSubscriptionControllerTest {

    @Test
    void returns200WithOwnedSubscriptions() throws Exception {
        given(service.findVisibleTo(USERNAME)).willReturn(List.of(
                new VisibleSubscriptionResponse(gymResponse(), MembershipRole.OWNER)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscription.id").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$[0].subscription.serviceName").value(CUSTOM_NAME))
                .andExpect(jsonPath("$[0].subscription.categoryName").value("Gym"))
                .andExpect(jsonPath("$[0].subscription.price").value(35.00))
                .andExpect(jsonPath("$[0].subscription.billingCycle").value("MONTHLY"))
                .andExpect(jsonPath("$[0].subscription.nextPaymentDate").value(NEXT_PAYMENT.toString()))
                .andExpect(jsonPath("$[0].role").value("OWNER"));
    }

    @Test
    void returns200WithEmptyArrayForUserWhoOwnsNothing() throws Exception {
        given(service.findOwnedBy(USERNAME)).willReturn(List.of());

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @WithAnonymousUser
    void returns401ForAnonymousUser() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());

        then(service).shouldHaveNoInteractions();
    }
}
