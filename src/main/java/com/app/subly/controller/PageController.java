package com.app.subly.controller;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.service.DashboardService;
import com.app.subly.service.ProviderService;
import com.app.subly.service.SubscriptionService;
import com.app.subly.service.UserService;
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
    private final UserService userService;

    public PageController(
            SubscriptionService subscriptionService,
            DashboardService dashboardService,
            ProviderService providerService,
            UserService userService) {
        this.subscriptionService = subscriptionService;
        this.dashboardService = dashboardService;
        this.providerService = providerService;
        this.userService = userService;
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        String username = principal.getName();
        model.addAttribute("summary", dashboardService.getSummary(username));
        model.addAttribute("upcoming", dashboardService.getUpcomingPayments(username));
        model.addAttribute(
                "activeCount", subscriptionService.findVisibleTo(username).size());
        return "dashboard";
    }

    @GetMapping("/subscriptions")
    public String subscriptions(Principal principal, Model model, @RequestParam(defaultValue = "active") String tab) {
        if (principal == null) return "redirect:/login";
        String username = principal.getName();
        var active = subscriptionService.findVisibleTo(username);
        var cancelled = subscriptionService.findCancelledVisibleTo(username);
        boolean showCancelled = CANCELLED_TAB.equals(tab);

        model.addAttribute("tab", showCancelled ? CANCELLED_TAB : "active");
        model.addAttribute("subscriptions", showCancelled ? cancelled : active);
        model.addAttribute("activeCount", active.size());
        model.addAttribute("cancelledCount", cancelled.size());
        model.addAttribute("zone", userService.getUserByUsername(username).getTimeZone());
        return "subscriptions";
    }

    @PostMapping("/subscriptions/{id}/cancel")
    public String cancelSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        if (principal == null) return "redirect:/login";
        subscriptionService.cancel(principal.getName(), id);
        redirect.addFlashAttribute("notice", "Subscription cancelled. You can find it under the Cancelled tab.");
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/reactivate")
    public String reactivateSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        if (principal == null) return "redirect:/login";
        subscriptionService.reactivate(principal.getName(), id);
        redirect.addFlashAttribute("notice", "Subscription reactivated. It's back in your active list.");
        return "redirect:/subscriptions";
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
    public String deleteSubscription(
            @PathVariable Long id, Principal principal, @RequestParam(defaultValue = "active") String tab) {
        subscriptionService.delete(principal.getName(), id);
        return CANCELLED_TAB.equals(tab) ? "redirect:/subscriptions?tab=" + CANCELLED_TAB : "redirect:/subscriptions";
    }
}
