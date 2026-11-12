package com.app.subly.controller;

import com.app.subly.dto.dashboard.DashboardResponse;
import com.app.subly.enums.Currency;
import com.app.subly.service.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final DashboardService dashboardService;
    private final InvitationService invitationService;
    private final PaymentService paymentService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Principal principal, Model model, @RequestParam(defaultValue = "EUR") Currency currency) {
        String username = principal.getName();
        logger.info("Preparing dashboard for username={} and currency={}", username, currency);
        DashboardResponse dashboard = dashboardService.getDashboard(username, currency);

        model.addAttribute("currencies", Currency.values());
        model.addAttribute("selectedCurrency", currency);
        model.addAttribute("summary", dashboard.summary());
        model.addAttribute("individualSummary", dashboard.individualSummary());
        model.addAttribute("sharedSummary", dashboard.sharedSummary());
        model.addAttribute("upcoming", dashboard.upcoming());
        model.addAttribute("savings", dashboard.savings());
        model.addAttribute("categories", dashboard.categories());
        model.addAttribute("receivedInvitations", invitationService.findReceived(username));
        logger.info("Successfully prepared dashboard for username={}", username);
        return "dashboard";
    }

    @GetMapping("/payments")
    public String payments(Principal principal, Model model) {
        logger.info("Preparing payment history for username={}", principal.getName());
        model.addAttribute("payments", paymentService.findHistory(principal.getName()));
        logger.info("Successfully prepared payment history for username={}", principal.getName());
        return "payments";
    }
}
