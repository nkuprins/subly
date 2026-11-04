package com.app.subly.dashboard;

import com.app.subly.config.SecurityConfig;
import com.app.subly.controller.DashboardController;
import com.app.subly.dto.dashboard.DashboardSummaryDto;
import com.app.subly.dto.dashboard.UpcomingPaymentDto;
import com.app.subly.models.subscription.Currency;
import com.app.subly.services.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "alice")
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Test
    void returnsSummaryOfAuthenticatedUser() throws Exception {
        given(dashboardService.getSummary("alice")).willReturn(
                new DashboardSummaryDto(
                        new BigDecimal("30.65"),
                        new BigDecimal("367.80"),
                        Currency.EUR
                )
        );

        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyTotal").value(30.65))
                .andExpect(jsonPath("$.yearlyTotal").value(367.80))
                .andExpect(jsonPath("$.currency").value("EUR"));

        then(dashboardService).should().getSummary("alice");
    }

    @Test
    void returnsUpcomingPaymentsOfAuthenticatedUser() throws Exception {
        LocalDate date = LocalDate.now().plusDays(10);
        given(dashboardService.getUpcomingPayments("alice")).willReturn(List.of(
                new UpcomingPaymentDto(5L, "Netflix",
                        new BigDecimal("9.99"), Currency.USD, date)
        ));

        mockMvc.perform(get("/api/dashboard/upcoming-payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].subscriptionId").value(5))
                .andExpect(jsonPath("$[0].providerName").value("Netflix"))
                .andExpect(jsonPath("$[0].amount").value(9.99))
                .andExpect(jsonPath("$[0].currency").value("USD"))
                .andExpect(jsonPath("$[0].nextPaymentDate").value(date.toString()));
    }

    @Test
    @WithAnonymousUser
    void summaryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/summary"))
                .andExpect(status().isUnauthorized());
        then(dashboardService).shouldHaveNoInteractions();
    }

    @Test
    @WithAnonymousUser
    void upcomingPaymentsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/dashboard/upcoming-payments"))
                .andExpect(status().isUnauthorized());
        then(dashboardService).shouldHaveNoInteractions();
    }
}
