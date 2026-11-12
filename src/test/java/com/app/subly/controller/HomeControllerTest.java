package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.hamcrest.Matchers.arrayContaining;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.DashboardResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.enums.Currency;
import com.app.subly.service.DashboardService;
import com.app.subly.service.InvitationService;
import com.app.subly.service.PaymentService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(HomeController.class)
@WithMockUser(username = ALICE)
class HomeControllerTest extends AbstractWebMvcTest {

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void dashboard_shouldRenderAllDashboardSectionsInEurByDefault() throws Exception {
        DashboardResponse dashboard = dashboardResponse(Currency.EUR);
        when(dashboardService.getDashboard(ALICE, Currency.EUR)).thenReturn(dashboard);
        when(invitationService.findReceived(ALICE)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("currencies", arrayContaining(Currency.values())))
                .andExpect(model().attribute("selectedCurrency", Currency.EUR))
                .andExpect(model().attribute("summary", dashboard.summary()))
                .andExpect(model().attribute("individualSummary", dashboard.individualSummary()))
                .andExpect(model().attribute("sharedSummary", dashboard.sharedSummary()))
                .andExpect(model().attribute("upcoming", dashboard.upcoming()))
                .andExpect(model().attribute("savings", dashboard.savings()))
                .andExpect(model().attribute("categories", dashboard.categories()))
                .andExpect(model().attribute("receivedInvitations", List.of()));

        verify(dashboardService).getDashboard(ALICE, Currency.EUR);
        verify(invitationService).findReceived(ALICE);
        verifyNoInteractions(paymentService);
    }

    @Test
    void dashboard_shouldAcceptRootUrl() throws Exception {
        when(dashboardService.getDashboard(ALICE, Currency.EUR)).thenReturn(dashboardResponse(Currency.EUR));
        when(invitationService.findReceived(ALICE)).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("selectedCurrency", Currency.EUR));

        verify(dashboardService).getDashboard(ALICE, Currency.EUR);
    }

    @Test
    void dashboard_shouldUseSelectedCurrency() throws Exception {
        DashboardResponse dashboard = dashboardResponse(Currency.GBP);
        when(dashboardService.getDashboard(ALICE, Currency.GBP)).thenReturn(dashboard);
        when(invitationService.findReceived(ALICE)).thenReturn(List.of());

        mockMvc.perform(get("/dashboard").param("currency", "GBP"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("selectedCurrency", Currency.GBP))
                .andExpect(model().attribute("summary", dashboard.summary()));

        verify(dashboardService).getDashboard(ALICE, Currency.GBP);
    }

    @Test
    void dashboard_shouldReturn400ForUnknownCurrency() throws Exception {
        mockMvc.perform(get("/dashboard").param("currency", "BTC")).andExpect(status().isBadRequest());

        verifyNoInteractions(dashboardService, invitationService, paymentService);
    }

    @Test
    void payments_shouldRenderPaymentHistoryForCurrentUser() throws Exception {
        when(paymentService.findHistory(ALICE)).thenReturn(List.of());

        mockMvc.perform(get("/payments"))
                .andExpect(status().isOk())
                .andExpect(view().name("payments"))
                .andExpect(model().attribute("payments", List.of()));

        verify(paymentService).findHistory(ALICE);
        verifyNoInteractions(dashboardService, invitationService);
    }

    @Test
    @WithAnonymousUser
    void dashboard_shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() == 302
                        || result.getResponse().getStatus() == 401));

        verifyNoInteractions(dashboardService, invitationService, paymentService);
    }

    @Test
    @WithAnonymousUser
    void payments_shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/payments"))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() == 302
                        || result.getResponse().getStatus() == 401));

        verifyNoInteractions(dashboardService, invitationService, paymentService);
    }

    private static DashboardResponse dashboardResponse(Currency currency) {
        DashboardSummaryResponse summary =
                new DashboardSummaryResponse(new BigDecimal("30.00"), new BigDecimal("360.00"), currency, 2);
        DashboardSummaryResponse individual =
                new DashboardSummaryResponse(new BigDecimal("20.00"), new BigDecimal("240.00"), currency, 1);
        DashboardSummaryResponse shared =
                new DashboardSummaryResponse(new BigDecimal("10.00"), new BigDecimal("120.00"), currency, 1);

        return new DashboardResponse(
                summary, individual, shared, List.of(), List.of(), new MoneyResponse(new BigDecimal("5.00"), currency));
    }
}
