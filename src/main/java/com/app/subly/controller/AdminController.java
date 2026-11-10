package com.app.subly.controller;

import com.app.subly.dto.admin.AdminStatisticsResponse;
import com.app.subly.dto.form.CreateCategoryForm;
import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.dto.provider.ProviderCategoryResponse;
import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.exception.category.CategoryAlreadyExistsException;
import com.app.subly.model.category.Category;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.service.AdminStatisticsService;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final ProviderService providerService;
    private final CategoryService categoryService;
    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/admin")
    public String catalogue(Model model, @RequestParam(defaultValue = "false") boolean providerCreated) {
        this.fillAdminPageByContent(model, providerCreated);
        return "admin";
    }

    @PostMapping("/admin/category/create")
    public String addCategory(
            @Valid @ModelAttribute("categoryCreationForm") CreateCategoryForm categoryForm,
            BindingResult bindingResult,
            RedirectAttributes attributes) {

        if (bindingResult.hasErrors()) {
            attributes.addFlashAttribute("categoryCreationForm", categoryForm);
            attributes.addFlashAttribute(
                    "categoryCreationError", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin";
        }

        try {
            this.categoryService.addNewCategory(categoryForm.getTitle());
        } catch (CategoryAlreadyExistsException ex) {
            attributes.addFlashAttribute("categoryCreationForm", categoryForm);
            attributes.addFlashAttribute("categoryCreationError", "The category with this name already exists");
        }

        return "redirect:/admin";
    }

    @GetMapping("/admin/providers/new")
    public String newProvider(Model model) {
        model.addAttribute("providerForm", new CreateProviderForm());
        model.addAttribute("categories", categoryService.findCategories());
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

    @GetMapping("/admin/statistics")
    @ResponseBody
    public AdminStatisticsResponse getStatistics() {
        return adminStatisticsService.getStatistics();
    }

    private void fillAdminPageByContent(Model model, boolean providerCreated) {

        List<SubscriptionProvider> providers = providerService.findCatalogProviders();
        model.addAttribute("providers", providers);
        model.addAttribute("providerCount", providers.size());
        model.addAttribute("providerCreated", providerCreated);
        model.addAttribute("statistics", adminStatisticsService.getStatistics());
        List<Category> categories = categoryService.findCategories();
        model.addAttribute("categories", categories);

        Map<Long, Long> categoryReferences = new HashMap<>();
        for (Category category : categories) {
            categoryReferences.put(category.getId(), 0L);
        }
        for (ProviderCategoryResponse reference : providerService.listGroupProvidersByCategory()) {
            categoryReferences.put(reference.categoryId(), reference.categoryCount());
        }
        model.addAttribute("categoryReferences", categoryReferences);

        if (!model.containsAttribute("categoryCreationError")) {
            model.addAttribute("categoryCreationError", "");
        }
        if (!model.containsAttribute("categoryCreationForm")) {
            model.addAttribute("categoryCreationForm", new CreateCategoryForm());
        }
    }
}
