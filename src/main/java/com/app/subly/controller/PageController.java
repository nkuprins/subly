package com.app.subly.controller;

import com.app.subly.dto.subscription.CreateCustomSubscriptionRequest;
import com.app.subly.dto.subscription.UpdateSubscriptionRequest;
import com.app.subly.exception.UserAlreadyExistsException;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.service.CategoryService;
import com.app.subly.service.DashboardService;
import com.app.subly.service.UserService;
import com.app.subly.service.SubscriptionService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;

@Controller
public class PageController {
    private final UserService userService;
    private final SubscriptionService subscriptionService;
    private final DashboardService dashboardService;
    private final CategoryService categoryService;

    public PageController(UserService userService, SubscriptionService subscriptionService, DashboardService dashboardService,
                          CategoryService categoryService) {
        this.userService = userService;
        this.subscriptionService = subscriptionService;
        this.dashboardService = dashboardService;
        this.categoryService = categoryService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register() {
        return "register";
    }

    @PostMapping("/register")
    public String createAccount(@RequestParam String username,
                                @RequestParam String name,
                                @RequestParam String email,
                                @RequestParam String password,
                                @RequestParam String confirmPassword,
                                Model model) {
        if (username.isBlank() || name.isBlank() || email.isBlank() || password.isBlank()) {
            model.addAttribute("registrationError", "All fields are required");
            return "register";
        }
        if (name.length() > 100) {
            model.addAttribute("registrationError", "Name must be 100 characters or fewer");
            return "register";
        }
        if (password.length() < 15 || password.length() > 64) {
            model.addAttribute("registrationError", "Password must be between 15 and 64 characters");
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("registrationError", "Passwords do not match");
            return "register";
        }
        try {
            userService.addNewUser(username, name, password, email, null);
            return "redirect:/login?registered";
        } catch (UserAlreadyExistsException exception) {
            model.addAttribute("registrationError", exception.getMessage());
            return "register";
        }
    }

    @GetMapping({"/", "/dashboard"})
    public String dashboard(Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        String username = principal.getName();
        model.addAttribute("summary", dashboardService.getSummary(username));
        model.addAttribute("upcoming", dashboardService.getUpcomingPayments(username));
        model.addAttribute("activeCount", subscriptionService.findVisibleTo(username).size());
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
        model.addAttribute("categories", categoryService.getAllCategories());
        return "subscription-form";
    }

    @PostMapping("/subscriptions")
    public String createSubscription(Principal principal,
                                     @RequestParam String name,
                                     @RequestParam Long categoryId,
                                     @RequestParam BigDecimal price,
                                     @RequestParam BillingCycle billingCycle,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                     LocalDate nextPaymentDate,
                                     @RequestParam(defaultValue = "false") boolean freeTrial) {
        subscriptionService.createCustom(principal.getName(), new CreateCustomSubscriptionRequest(name, categoryId, price,
                billingCycle, nextPaymentDate, freeTrial));
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
    public String updateSubscription(@PathVariable Long id, Principal principal,
                                     @RequestParam BigDecimal price,
                                     @RequestParam BillingCycle billingCycle,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate nextPaymentDate,
                                     @RequestParam(defaultValue = "false") boolean freeTrial) {
        subscriptionService.update(principal.getName(), id, new UpdateSubscriptionRequest(price, billingCycle,
                nextPaymentDate, freeTrial));
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/delete")
    public String deleteSubscription(@PathVariable Long id, Principal principal) {
        subscriptionService.delete(principal.getName(), id);
        return "redirect:/subscriptions";

    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
    }
}
