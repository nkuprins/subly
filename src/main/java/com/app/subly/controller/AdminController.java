package com.app.subly.controller;

import com.app.subly.dto.admin.AdminStatisticsResponse;
import com.app.subly.dto.category.CategoryResponse;
import com.app.subly.dto.form.CreateCategoryForm;
import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.dto.provider.ProviderCategoryResponse;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.enums.ProviderType;
import com.app.subly.exception.category.CategoryAlreadyExistsException;
import com.app.subly.service.AdminStatisticsService;
import com.app.subly.service.CategoryService;
import com.app.subly.service.ProviderService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final ProviderService providerService;
    private final CategoryService categoryService;
    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/admin")
    public String catalogue(Model model, @RequestParam(defaultValue = "false") boolean providerCreated) {
        log.info("Preparing admin catalogue page, providerCreated={}", providerCreated);
        this.fillAdminPageByContent(model, providerCreated);
        log.info("Successfully prepared admin catalogue page");
        return "admin";
    }

    @PostMapping("/admin/category/create")
    public String addCategory(
            @Valid @ModelAttribute("categoryCreationForm") CreateCategoryForm categoryForm,
            BindingResult bindingResult,
            RedirectAttributes attributes) {

        log.info("Creating category from admin page");

        if (bindingResult.hasErrors()) {
            log.info("Category creation form has validation errors");
            attributes.addFlashAttribute("categoryCreationForm", categoryForm);
            attributes.addFlashAttribute(
                    "categoryCreationError", bindingResult.getFieldError().getDefaultMessage());
            return "redirect:/admin";
        }

        try {
            this.categoryService.create(categoryForm.getTitle());
            log.info("Category created successfully");
        } catch (CategoryAlreadyExistsException ex) {
            log.info("Category creation rejected because the category already exists");
            attributes.addFlashAttribute("categoryCreationForm", categoryForm);
            attributes.addFlashAttribute("categoryCreationError", "The category with this name already exists");
        }

        return "redirect:/admin";
    }

    @GetMapping("/admin/providers/new")
    public String newProvider(Model model) {
        log.info("Preparing admin provider creation page");
        model.addAttribute("providerForm", new CreateProviderForm());
        model.addAttribute("categories", categoryService.findAll());
        return "admin-provider-form";
    }

    @PostMapping("/admin/providers")
    public String addNewProvider(
            @Valid @ModelAttribute("providerForm") CreateProviderForm form,
            BindingResult bindingResult,
            Model model,
            Principal principal) {

        log.info("Creating catalogue provider for username={}", principal.getName());

        if (bindingResult.hasErrors()) {
            log.info("Catalogue provider form has validation errors for username={}", principal.getName());
            model.addAttribute("categories", categoryService.findAll());
            return "admin-provider-form"; // return the user back on the page
        }

        providerService.create(principal.getName(), form, ProviderType.CATALOG);
        log.info("Catalogue provider created successfully for username={}", principal.getName());

        return "redirect:/admin";
    }

    @GetMapping("/admin/statistics")
    @ResponseBody
    public AdminStatisticsResponse getStatistics() {
        log.info("Retrieving admin statistics");
        return adminStatisticsService.getStatistics();
    }

    private void fillAdminPageByContent(Model model, boolean providerCreated) {

        log.debug("Loading admin catalogue data");

        List<SubscriptionProvider> providers = providerService.findCatalogProviders();
        List<CategoryResponse> categories = categoryService.findAll();

        model.addAttribute("providers", providers);
        model.addAttribute("providerCount", providers.size());
        model.addAttribute("providerCreated", providerCreated);
        model.addAttribute("statistics", adminStatisticsService.getStatistics());
        model.addAttribute("categories", categories);

        Map<Long, Long> categoryReferences = new HashMap<>();
        for (CategoryResponse category : categories) {
            categoryReferences.put(category.id(), 0L);
        }
        for (ProviderCategoryResponse reference : providerService.findProviderCountsByCategory()) {
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
