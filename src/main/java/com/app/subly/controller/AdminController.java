package com.app.subly.controller;

import com.app.subly.model.subscription.SubscriptionProvider;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final ProviderService catalogService;
    private final CategoryService categoryService;

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
        model.addAttribute("categories", categoryService.findCategories());
        return "admin-provider-form";
    }
}
