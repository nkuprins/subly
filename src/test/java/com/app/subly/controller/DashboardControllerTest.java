package com.app.subly.controller;

import static com.app.subly.testdata.TestProviders.CATALOG_PROVIDER_NAME;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.PaidMonthResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.subscription.Currency;
import com.app.subly.service.DashboardService;
import com.app.subly.service.PaymentService;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(DashboardController.class)
@WithMockUser(username = ALICE)
class DashboardControllerTest extends AbstractWebMvcTest {

    static final String SUMMARY_URL = "/api/dashboard/summary";
    static final String UPCOMING_PAYMENTS_URL = "/api/dashboard/upcoming-payments";
    static final String SAVINGS_URL = "/api/dashboard/savings";
    static final String PAID_PER_MONTH_URL = "/api/dashboard/paid-per-month";

    @MockitoBean
    DashboardService dashboardService;

    @MockitoBean
    PaymentService paymentService;

    @Test
    void getSummary_shouldReturnSummaryOfAuthenticatedUser() throws Exception {
        DashboardSummaryResponse summary =
                new DashboardSummaryResponse(new BigDecimal("30.65"), new BigDecimal("367.80"), Currency.EUR, 2);
        when(dashboardService.getSummary(ALICE)).thenReturn(summary);

        mockMvc.perform(get(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.monthlyTotal").value(summary.monthlyTotal().doubleValue()))
                .andExpect(jsonPath("$.yearlyTotal").value(summary.yearlyTotal().doubleValue()))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void getUpcomingPayments_shouldReturnPaymentsOfAuthenticatedUser() throws Exception {
        UpcomingPaymentResponse payment = new UpcomingPaymentResponse(
                5L, CATALOG_PROVIDER_NAME, DEFAULT_PRICE, Currency.USD, DEFAULT_NEXT_PAYMENT_DATE, 10L);
        when(dashboardService.getUpcomingPayments(ALICE)).thenReturn(List.of(payment));

        mockMvc.perform(get(UPCOMING_PAYMENTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscriptionId").value(payment.subscriptionId()))
                .andExpect(jsonPath("$[0].providerName").value(payment.providerName()))
                .andExpect(jsonPath("$[0].amount").value(payment.amount().doubleValue()))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].nextPaymentDate")
                        .value(payment.nextPaymentDate().toString()))
                .andExpect(jsonPath("$[0].daysLeft").value(payment.daysLeft()));
    }

    @Test
    void getSavings_shouldReturnSavingsOfAuthenticatedUser() throws Exception {
        when(dashboardService.getSavings(ALICE)).thenReturn(new MoneyResponse(new BigDecimal("20.00"), Currency.EUR));

        mockMvc.perform(get(SAVINGS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(20.00))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void getPaidPerMonth_shouldReturnPaymentsOfAuthenticatedUser() throws Exception {
        PaidMonthResponse month =
                new PaidMonthResponse(YearMonth.of(2026, 11), new MoneyResponse(new BigDecimal("4.50"), Currency.USD));
        when(paymentService.getPaidPerMonth(ALICE, Currency.USD)).thenReturn(List.of(month));

        mockMvc.perform(get(PAID_PER_MONTH_URL).param("currency", "USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].month").value("2026-11"))
                .andExpect(jsonPath("$[0].paid.amount").value(4.50))
                .andExpect(jsonPath("$[0].paid.currency").value("USD"));
    }

    @Test
    @WithAnonymousUser
    void getSummary_shouldReturn401ForAnonymousUser() throws Exception {
        mockMvc.perform(get(SUMMARY_URL)).andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }

    @Test
    @WithAnonymousUser
    void getUpcomingPayments_shouldReturn401ForAnonymousUser() throws Exception {
        mockMvc.perform(get(UPCOMING_PAYMENTS_URL)).andExpect(status().isUnauthorized());

        verifyNoInteractions(dashboardService);
    }
}
