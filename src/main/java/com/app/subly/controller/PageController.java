package com.app.subly.controller;

import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.dto.subscription.VisibleSubscriptionResponse;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class PageController {

    private static final String CANCELLED_TAB = "cancelled";

    private final SubscriptionService subscriptionService;
    private final DashboardService dashboardService;
    private final ProviderService providerService;
    private final InvitationService invitationService;
    private final CategoryService categoryService;
    private final PaymentService paymentService;

    public PageController(
            SubscriptionService subscriptionService,
            DashboardService dashboardService,
            ProviderService providerService,
            InvitationService invitationService,
            CategoryService categoryService,
            PaymentService paymentService) {
        this.subscriptionService = subscriptionService;
        this.dashboardService = dashboardService;
        this.providerService = providerService;
        this.invitationService = invitationService;
        this.categoryService = categoryService;
        this.paymentService = paymentService;
    }

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

    @GetMapping("/subscriptions")
    public String subscriptions(Principal principal, Model model, @RequestParam(defaultValue = "active") String tab) {
        String username = principal.getName();
        var active = subscriptionService.findCurrent(username);
        var cancelled = subscriptionService.findCancelled(username);
        boolean showCancelled = CANCELLED_TAB.equals(tab);

        model.addAttribute("tab", showCancelled ? CANCELLED_TAB : "active");
        model.addAttribute("subscriptions", showCancelled ? cancelled : active);
        model.addAttribute("activeCount", active.size());
        model.addAttribute("cancelledCount", cancelled.size());
        model.addAttribute("shares", subscriptionService.currentShares(username));
        return "subscriptions";
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public String cancelSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        subscriptionService.cancel(principal.getName(), id);
        redirect.addFlashAttribute("notice", "Subscription cancelled. You can find it under the Cancelled tab.");
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/reactivate")
    public String reactivateSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        subscriptionService.reactivate(principal.getName(), id);
        redirect.addFlashAttribute("notice", "Subscription reactivated. It's back in your active list.");
        return "redirect:/subscriptions";
    }

    @GetMapping("/subscriptions/new")
    public String newSubscriptions(Principal principal, Model model) {
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
        var sub = subscriptionService.findCurrent(principal.getName()).stream()
                .map(VisibleSubscriptionResponse::subscription)
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElse(null);
        if (sub == null) return "redirect:/subscriptions";
        model.addAttribute("editId", id);
        model.addAttribute("sub", sub);
        return "subscription-form";
    }

    @GetMapping("/subscriptions/{id}/participants")
    public String participants(@PathVariable Long id, Principal principal, Model model) {
        var sub = subscriptionService.findCurrent(principal.getName()).stream()
                .map(VisibleSubscriptionResponse::subscription)
                .filter(s -> s.id().equals(id))
                .findFirst()
                .orElse(null);
        if (sub == null) return "redirect:/subscriptions";
        model.addAttribute("sub", sub);
        model.addAttribute("participants", subscriptionService.findParticipants(principal.getName(), id));
        model.addAttribute("shares", subscriptionService.participantShares(principal.getName(), id));
        return "participants";
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

    @PostMapping("/subscriptions/{id}/leave")
    public String leaveSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            subscriptionService.leave(principal.getName(), id);
            redirectAttributes.addFlashAttribute("notice", "You have left the group.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/delete")
    public String deleteSubscription(
            @PathVariable Long id, Principal principal, @RequestParam(defaultValue = "active") String tab) {
        subscriptionService.delete(principal.getName(), id);
        return CANCELLED_TAB.equals(tab) ? "redirect:/subscriptions?tab=" + CANCELLED_TAB : "redirect:/subscriptions";
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
