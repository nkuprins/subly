package com.app.subly.controller;

import com.app.subly.dto.user.PasswordRules;
import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.model.user.User;
import com.app.subly.service.UserService;
import com.app.subly.util.TimeZoneGenerator;
import java.security.Principal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@AllArgsConstructor
public class UserPageController {
    private final UserService userService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/access-denied")
    public String accessDenied() {
        return "access-denied";
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
        if (username.length() > User.USERNAME_MAX_LENGTH) {
            model.addAttribute(
                    "registrationError", "Username must be " + User.USERNAME_MAX_LENGTH + " characters or fewer");
            return "register";
        }
        if (name.length() > User.NAME_MAX_LENGTH) {
            model.addAttribute("registrationError", "Name must be " + User.NAME_MAX_LENGTH + " characters or fewer");
            return "register";
        }
        if (password.length() < PasswordRules.MIN_LENGTH || password.length() > PasswordRules.MAX_LENGTH) {
            model.addAttribute(
                    "registrationError",
                    "Password must be between " + PasswordRules.MIN_LENGTH + " and " + PasswordRules.MAX_LENGTH
                            + " characters");
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

    @GetMapping("/edit/me")
    public String returnEditPage(Principal principal, Model model) {
        User user = userService.getUserByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("timeZones", TimeZoneGenerator.getAllFilteredTimeZone());
        return "edit-user";
    }
}
