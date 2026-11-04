package com.app.subly.controller;

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
        given(service.findOwnedBy(USERNAME)).willReturn(List.of(gymResponse()));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(SUBSCRIPTION_ID))
                .andExpect(jsonPath("$[0].providerName").value(CUSTOM_NAME))
                .andExpect(jsonPath("$[0].categoryName").value("Gym"))
                .andExpect(jsonPath("$[0].price").value(35.00))
                .andExpect(jsonPath("$[0].billingCycle").value("MONTHLY"))
                .andExpect(jsonPath("$[0].nextPaymentDate").value(NEXT_PAYMENT.toString()));
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
