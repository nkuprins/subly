package com.app.subly.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PageController {

    @GetMapping({"/", "/dashboard"})
    public String dashboard() {
        return "dashboard";
    }

    @GetMapping("/subscriptions")
    public String subscriptions() {
        return "subscriptions";
    }

    @GetMapping("/subscriptions/new")
    public String newSubscriptions() {
        return "subscription-form";
    }

    @GetMapping("/subscriptions/{id}/edit")
    public String editSubscriptions(@PathVariable Long id, Model model) {
        model.addAttribute("editId", id);
        return "subscription-form";
    }
}
