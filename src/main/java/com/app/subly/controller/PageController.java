package com.app.subly.controller;

import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.service.*;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {

    private final SubscriptionService subscriptionService;
    private final DashboardService dashboardService;
    private final ProviderService providerService;
    private final InvitationService invitationService;
    private final CategoryService categoryService;

    public PageController(
            SubscriptionService subscriptionService,
            DashboardService dashboardService,
            ProviderService providerService,
            InvitationService invitationService,
            CategoryService categoryservice) {
        this.subscriptionService = subscriptionService;
        this.dashboardService = dashboardService;
        this.providerService = providerService;
        this.invitationService = invitationService;
        this.categoryService = categoryservice;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        String username = principal.getName();
        model.addAttribute("summary", dashboardService.getSummary(username));
        model.addAttribute("upcoming", dashboardService.getUpcomingPayments(username));
        model.addAttribute(
                "activeCount", subscriptionService.findVisibleTo(username).size());
        model.addAttribute("categories", dashboardService.getSpendingByCategory(username));
        model.addAttribute("receivedInvitations", invitationService.findReceived(username));
        return "dashboard";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("subscriptions", subscriptionService.findVisibleTo(principal.getName()));
        return "subscriptions";
    }

    @GetMapping("/subscriptions/new")
    public String newSubscriptions(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("providers", providerService.findVisibleTo(principal.getName()));
        return "subscription-form";
    }

    @PostMapping("/subscriptions")
    public String createSubscription(
            Principal principal,
            @RequestParam Long providerId,
            @RequestParam BigDecimal price,
            @RequestParam BillingCycle billingCycle,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextPaymentDate,
            @RequestParam(defaultValue = "false") boolean freeTrial,
            @RequestParam(defaultValue = "1") Integer maxParticipants) {
        subscriptionService.create(
                principal.getName(),
                new CreateSubscriptionRequest(
                        providerId, price, billingCycle, nextPaymentDate, freeTrial, maxParticipants));
        return "redirect:/subscriptions";
    }

    @GetMapping("/subscriptions/{id}/edit")
    public String editSubscriptions(@PathVariable Long id, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        var sub = subscriptionService.findVisibleTo(principal.getName()).stream()
                .map(v -> v.subscription())
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElse(null);
        if (sub == null) return "redirect:/subscriptions";
        model.addAttribute("editId", id);
        model.addAttribute("sub", sub);
        return "subscription-form";
    }

    @PostMapping("/subscriptions/{id}")
    public String updateSubscription(
            @PathVariable Long id,
            Principal principal,
            @RequestParam BigDecimal price,
            @RequestParam BillingCycle billingCycle,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextPaymentDate,
            @RequestParam(defaultValue = "false") boolean freeTrial) {
        subscriptionService.update(
                principal.getName(),
                id,
                new UpdateSubscriptionRequest(price, billingCycle, nextPaymentDate, freeTrial));
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/delete")
    public String deleteSubscription(@PathVariable Long id, Principal principal) {
        subscriptionService.delete(principal.getName(), id);
        return "redirect:/subscriptions";
    }

    @GetMapping("/providers/new")
    public String newProvider(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
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
        if (principal == null) return "redirect:/login";
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
