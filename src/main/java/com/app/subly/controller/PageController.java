package com.app.subly.controller;

import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PageController {
    private final UserService userService;

    public PageController(UserService userService) {
        this.userService = userService;
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
    public String createAccount(
            @RequestParam String username,
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
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/subscriptions")
    public String subscriptions() {
        return "subscriptions";
    }

    @GetMapping("/subscriptions/new")
    public String newSubscriptions() {
        return "subscription-form";
    }

    @GetMapping("/subscriptions/{id}/edit")
    public String editSubscriptions(@PathVariable Long id, Model model) {
        model.addAttribute("editId", id);
        return "subscription-form";
    }
}
