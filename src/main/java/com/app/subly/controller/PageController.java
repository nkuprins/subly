package com.app.subly.controller;

import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.Currency;
import com.app.subly.service.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final DashboardService dashboardService;
    private final ProviderService providerService;
    private final InvitationService invitationService;
    private final CategoryService categoryService;
    private final PaymentService paymentService;

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Principal principal, Model model, @RequestParam(defaultValue = "EUR") Currency currency) {
        String username = principal.getName();

        model.addAttribute("selectedCurrency", currency);
        model.addAttribute("summary", dashboardService.getSummary(username, currency));
        model.addAttribute("upcoming", dashboardService.getUpcomingPayments(username, currency));
        model.addAttribute("savings", dashboardService.getSavings(username, currency));
        model.addAttribute("categories", dashboardService.getSpendingByCategory(username, currency));
        model.addAttribute("receivedInvitations", invitationService.findReceived(username));

        model.addAttribute("individualSummary", dashboardService.getSummary(username, currency, false));
        model.addAttribute("sharedSummary", dashboardService.getSummary(username, currency, true));
        return "dashboard";
    }

    @GetMapping("/payments")
    public String payments(Principal principal, Model model) {
        model.addAttribute("payments", paymentService.findHistory(principal.getName()));
        return "payments";
    }

    @GetMapping("/providers/new")
    public String newProvider(Model model) {
        model.addAttribute("categories", categoryService.getAllCategories());
        return "provider-form";
    }

    @PostMapping("/providers")
    public String createProvider(
            Principal principal,
            @RequestParam String name,
            @RequestParam Long categoryId,
            @RequestParam(required = false) String logoPath,
            Model model) {
        try {
            providerService.create(
                    principal.getName(), new ProviderRequest(name, categoryId, logoPath), ProviderType.CUSTOM);
            return "redirect:/subscriptions/new";
        } catch (RuntimeException ex) {
            model.addAttribute("categories", categoryService.getAllCategories());
            model.addAttribute("error", ex.getMessage());
            return "provider-form";
        }
    }
}
