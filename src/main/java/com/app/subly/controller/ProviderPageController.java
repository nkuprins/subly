package com.app.subly.controller;

import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ProviderPageController {
    private final CategoryService categoryService;
    private final ProviderService providerService;

    @GetMapping("/providers/new")
    public String newProvider(Model model) {
        model.addAttribute("providerForm", new CreateProviderForm());
        model.addAttribute("categories", categoryService.findCategories());
        return "provider-form";
    }

    @PostMapping("/providers")
    public String createProvider(
            Principal principal,
            @Valid @ModelAttribute("providerForm") CreateProviderForm form,
            BindingResult bindingResult,
            Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories", categoryService.findCategories());
            return "provider-form";
        }

        providerService.create(
                principal.getName(),
                new ProviderRequest(form.getName(), form.getCategoryId(), form.getLogoPath()),
                ProviderType.CUSTOM);
        return "redirect:/subscriptions/new";
    }
}
