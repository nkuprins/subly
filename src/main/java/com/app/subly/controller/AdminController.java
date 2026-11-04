package com.app.subly.controller;

import com.app.subly.dto.admin.CreateCatalogServiceForm;
import com.app.subly.exceptions.subscription.CategoryNotFoundException;
import com.app.subly.models.subscription.ServiceProvider;
import com.app.subly.services.AdminCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final AdminCatalogService catalogService;

    @GetMapping("/admin")
    public String catalogue(Model model, @RequestParam(defaultValue = "false") boolean created) {
        List<ServiceProvider> services = catalogService.findCatalogServices();
        model.addAttribute("services", services);
        model.addAttribute("serviceCount", services.size());
        model.addAttribute("created", created);
        return "admin";
    }

    @GetMapping("/admin/services/new")
    public String newService(Model model) {
        model.addAttribute("serviceForm", new CreateCatalogServiceForm());
        model.addAttribute("categories", catalogService.findCategories());
        return "admin-service-form";
    }

    @PostMapping("/admin/services")
    public String createService(@Valid @ModelAttribute("serviceForm") CreateCatalogServiceForm form,
                                BindingResult bindingResult, Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return showForm(model);
        }

        try {
            catalogService.createCatalogService(form.getName(), form.getCategoryId(), form.getLogoPath());
        } catch (CategoryNotFoundException exception) {
            bindingResult.rejectValue("categoryId", "category.invalid", "Choose a category that still exists.");
            return showForm(model);
        }

        redirectAttributes.addAttribute("created", true);
        return "redirect:/admin";
    }

    private String showForm(Model model) {
        model.addAttribute("categories", catalogService.findCategories());
        return "admin-service-form";
    }
}
