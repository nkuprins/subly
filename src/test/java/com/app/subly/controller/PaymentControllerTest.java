package com.app.subly.controller;

import static com.app.subly.testdata.TestProviders.CATALOG_PROVIDER_NAME;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.payment.PaymentResponse;
import com.app.subly.model.subscription.Currency;
import com.app.subly.service.PaymentService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(PaymentController.class)
@WithMockUser(username = ALICE)
class PaymentControllerTest extends AbstractWebMvcTest {

    static final String URL = "/api/payments";

    @MockitoBean
    PaymentService paymentService;

    @Test
    void listMine_shouldReturnPaymentsOfAuthenticatedUser() throws Exception {
        PaymentResponse payment = new PaymentResponse(
                5L, CATALOG_PROVIDER_NAME, LocalDate.of(2026, 10, 4), new BigDecimal("3.34"), Currency.EUR);
        when(paymentService.findHistory(ALICE)).thenReturn(List.of(payment));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscriptionId").value(payment.subscriptionId()))
                .andExpect(jsonPath("$[0].providerName").value(payment.providerName()))
                .andExpect(jsonPath("$[0].paidOn").value(payment.paidOn().toString()))
                .andExpect(jsonPath("$[0].amount").value(payment.amount().doubleValue()))
                .andExpect(jsonPath("$[0].currency").value("EUR"));
    }
}
