package com.app.subly.controller;

import com.app.subly.dto.user.PasswordRules;
import com.app.subly.entity.user.User;
import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.service.UserService;
import com.app.subly.util.TimeZoneGenerator;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class UserPageController {
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        log.info("Preparing login page");
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        log.info("Preparing access denied page");
        return "access-denied";
    }

    @GetMapping("/register")
    public String register() {
        log.info("Preparing registration page");
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
        log.info("Processing account registration for username={}", username);
        if (username.isBlank() || name.isBlank() || email.isBlank() || password.isBlank()) {
            log.info("Registration rejected because required fields are missing");
            model.addAttribute("registrationError", "All fields are required");
            return "register";
        }
        if (username.length() > User.USERNAME_MAX_LENGTH) {
            log.info("Registration rejected because username exceeds maximum length");
            model.addAttribute(
                    "registrationError", "Username must be " + User.USERNAME_MAX_LENGTH + " characters or fewer");
            return "register";
        }
        if (name.length() > User.NAME_MAX_LENGTH) {
            log.info("Registration rejected because name exceeds maximum length");
            model.addAttribute("registrationError", "Name must be " + User.NAME_MAX_LENGTH + " characters or fewer");
            return "register";
        }
        if (password.length() < PasswordRules.MIN_LENGTH || password.length() > PasswordRules.MAX_LENGTH) {
            log.info("Registration rejected because password does not meet length requirements");
            model.addAttribute(
                    "registrationError",
                    "Password must be between " + PasswordRules.MIN_LENGTH + " and " + PasswordRules.MAX_LENGTH
                            + " characters");
            return "register";
        }
        if (!password.equals(confirmPassword)) {
            log.info("Registration rejected because passwords do not match");
            model.addAttribute("registrationError", "Passwords do not match");
            return "register";
        }
        try {
            userService.create(username, name, password, email, null);
            log.info("Account registered successfully for username={}", username);
            return "redirect:/login?registered";
        } catch (UserAlreadyExistsException exception) {
            log.info("Registration rejected because username or email is already in use");
            model.addAttribute("registrationError", exception.getMessage());
            return "register";
        }
    }

    @GetMapping("/edit/me")
    public String returnEditPage(Principal principal, Model model) {
        log.info("Preparing profile edit page for username={}", principal.getName());
        User user = userService.getUser(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("timeZones", TimeZoneGenerator.getAllFilteredTimeZone());
        log.info("Successfully prepared profile edit page for username={}", principal.getName());
        return "edit-user";
    }
}
