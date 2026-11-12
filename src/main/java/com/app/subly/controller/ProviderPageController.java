package com.app.subly.controller;

import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.enums.ProviderType;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ProviderPageController {
    private final CategoryService categoryService;
    private final ProviderService providerService;

    @GetMapping("/providers/new")
    public String newProvider(Model model) {
        log.info("Preparing custom provider creation page");
        model.addAttribute("providerForm", new CreateProviderForm());
        model.addAttribute("categories", categoryService.findAll());
        return "provider-form";
    }

    @PostMapping("/providers")
    public String createProvider(
            Principal principal,
            @Valid @ModelAttribute("providerForm") CreateProviderForm form,
            BindingResult bindingResult,
            Model model) {

        log.info("Creating custom provider for username={}", principal.getName());

        if (bindingResult.hasErrors()) {
            log.info("Custom provider form has validation errors for username={}", principal.getName());
            model.addAttribute("categories", categoryService.findAll());
            return "provider-form";
        }

        providerService.create(principal.getName(), form, ProviderType.CUSTOM);
        log.info("Custom provider created successfully for username={}", principal.getName());
        return "redirect:/subscriptions/new";
    }
}
