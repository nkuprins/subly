package com.app.subly.controller;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.service.ProviderService;
import com.app.subly.service.SubscriptionService;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/subscriptions")
public class SubscriptionPageController {
    private final SubscriptionService subscriptionService;
    private final ProviderService providerService;

    private static final String CANCELLED_TAB = "cancelled";

    @GetMapping()
    public String subscriptions(Principal principal, Model model, @RequestParam(defaultValue = "active") String tab) {
        log.info("Preparing subscription page");
        String username = principal.getName();
        var active = subscriptionService.findActive(username);
        var cancelled = subscriptionService.findCancelled(username);
        boolean showCancelled = CANCELLED_TAB.equals(tab);

        model.addAttribute("tab", showCancelled ? CANCELLED_TAB : "active");
        model.addAttribute("subscriptions", showCancelled ? cancelled : active);
        model.addAttribute("activeCount", active.size());
        model.addAttribute("cancelledCount", cancelled.size());
        log.info("Successfully prepared subscription page");
        return "subscriptions";
    }

    @GetMapping("/new")
    public String newSubscriptions(Principal principal, Model model) {
        model.addAttribute("providers", providerService.findVisibleProviders(principal.getName()));
        return "subscription-form";
    }

    @PostMapping()
    public String createSubscription(
            Principal principal,
            @RequestParam Long providerId,
            @RequestParam BigDecimal price,
            @RequestParam(defaultValue = "EUR") Currency currency,
            @RequestParam BillingCycle billingCycle,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextPaymentDate,
            @RequestParam(defaultValue = "false") boolean freeTrial,
            @RequestParam(defaultValue = "1") Integer maxParticipants) {

        log.info(
                "Creating subscription: providerId={}, billingCycle={}, freeTrial={}, maxParticipants={}",
                providerId,
                billingCycle,
                freeTrial,
                maxParticipants);

        subscriptionService.create(
                principal.getName(),
                new CreateSubscriptionRequest(
                        providerId, price, currency, billingCycle, nextPaymentDate, freeTrial, maxParticipants));

        log.info("The subscription has been created successfully");
        return "redirect:/subscriptions";
    }

    @PostMapping("/{id}/cancel")
    public String cancelSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        log.info("Canceling the subscription with id={}", id);
        subscriptionService.cancel(principal.getName(), id);
        redirect.addFlashAttribute("notice", "Subscription cancelled. You can find it under the Cancelled tab.");
        log.info("Successfully canceled the subscription");
        return "redirect:/subscriptions";
    }

    @PostMapping("/{id}/reactivate")
    public String reactivateSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        log.info("Reactivating the subscription with id={}", id);
        subscriptionService.reactivate(principal.getName(), id);
        redirect.addFlashAttribute("notice", "Subscription reactivated. It's back in your active list.");
        log.info("Successfully reactivated the subscription");
        return "redirect:/subscriptions";
    }

    @GetMapping("/{id}/edit")
    public String editSubscriptions(@PathVariable Long id, Principal principal, Model model) {
        log.info("Filling editing page for the subscription id={}", id);
        model.addAttribute("editId", id);
        model.addAttribute("sub", subscriptionService.getActive(principal.getName(), id));
        return "subscription-form";
    }

    @PostMapping("/{id}")
    public String updateSubscription(
            @PathVariable Long id,
            Principal principal,
            @RequestParam BigDecimal price,
            @RequestParam BillingCycle billingCycle,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextPaymentDate,
            @RequestParam(defaultValue = "false") boolean freeTrial) {

        UpdateSubscriptionRequest request =
                new UpdateSubscriptionRequest(price, billingCycle, nextPaymentDate, freeTrial);
        log.info("Updating the subscription id={}: {}", id, request);
        subscriptionService.update(principal.getName(), id, request);
        log.info("Successfully updated the subscription id={}", id);

        return "redirect:/subscriptions";
    }

    @PostMapping("/{id}/delete")
    public String deleteSubscription(
            @PathVariable Long id, Principal principal, @RequestParam(defaultValue = "active") String tab) {
        log.info("Deleting subscription id={}", id);
        subscriptionService.delete(principal.getName(), id);
        log.info("Successfully deleted the subscription id={}", id);
        return CANCELLED_TAB.equals(tab) ? "redirect:/subscriptions?tab=" + CANCELLED_TAB : "redirect:/subscriptions";
    }
}
