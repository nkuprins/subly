package com.app.subly.controller;

import com.app.subly.dto.category.CategoryResponse;
import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.service.DashboardService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardSummaryResponse getSummary(Principal principal) {
        return dashboardService.getSummary(principal.getName());
    }

    @GetMapping("/upcoming-payments")
    public List<UpcomingPaymentResponse> getUpcomingPayments(Principal principal) {
        return dashboardService.getUpcomingPayments(principal.getName());
    }

    @GetMapping("/categories")
    public List<CategorySpendingResponse> getSpendingByCategory(Principal principal) {
        return dashboardService.getSpendingByCategory(principal.getName());
    }
}
