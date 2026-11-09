package com.app.subly.controller;

import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.model.category.Category;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
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
public class AdminController {

    private final ProviderService providerService;
    private final CategoryService categoryService;

    @ModelAttribute("categories")
    public List<Category> getCategories() {
        return categoryService.findCategories();
    }

    @GetMapping("/admin")
    public String catalogue(Model model, @RequestParam(defaultValue = "false") boolean created) {
        List<SubscriptionProvider> providers = providerService.findCatalogProviders();
        model.addAttribute("providers", providers);
        model.addAttribute("providerCount", providers.size());
        model.addAttribute("created", created);
        return "admin";
    }

    @GetMapping("/admin/providers/new")
    public String newProvider(Model model) {
        model.addAttribute("providerForm", new CreateProviderForm());
        model.addAttribute("categories");
        return "admin-provider-form";
    }

    @PostMapping("/admin/providers")
    public String addNewProvider(
            @Valid @ModelAttribute("providerForm") CreateProviderForm form,
            BindingResult bindingResult,
            Model model,
            Principal principal) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("categories");
            return "admin-provider-form"; // return the user back on the page
        }

        providerService.create(
                principal.getName(),
                new ProviderRequest(form.getName(), form.getCategoryId(), form.getLogoPath()),
                ProviderType.CATALOG);

        return "redirect:/admin";
    }
}
