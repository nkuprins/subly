package com.app.subly.controller;

import com.app.subly.dto.admin.CreateCatalogProviderForm;
import com.app.subly.exception.subscription.CategoryNotFoundException;
import com.app.subly.model.subscription.SubscriptionProvider;
import com.app.subly.service.AdminCatalogService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AdminCatalogService catalogService;

    @GetMapping("/admin")
    public String catalogue(Model model, @RequestParam(defaultValue = "false") boolean created) {
        List<SubscriptionProvider> providers = catalogService.findCatalogProviders();
        model.addAttribute("providers", providers);
        model.addAttribute("providerCount", providers.size());
        model.addAttribute("created", created);
        return "admin";
    }

    @GetMapping("/admin/providers/new")
    public String newProvider(Model model) {
        model.addAttribute("providerForm", new CreateCatalogProviderForm());
        model.addAttribute("categories", catalogService.findCategories());
        return "admin-provider-form";
    }

    @PostMapping("/admin/providers")
    public String createProvider(
            @Valid @ModelAttribute("providerForm") CreateCatalogProviderForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(model);
        }

        try {
            catalogService.createCatalogProvider(form.getName(), form.getCategoryId(), form.getLogoPath());
        } catch (CategoryNotFoundException exception) {
            bindingResult.rejectValue("categoryId", "category.invalid", "Choose a category that still exists.");
            return showForm(model);
        }

        redirectAttributes.addAttribute("created", true);
        return "redirect:/admin";
    }

    private String showForm(Model model) {
        model.addAttribute("categories", catalogService.findCategories());
        return "admin-provider-form";
    }
}
