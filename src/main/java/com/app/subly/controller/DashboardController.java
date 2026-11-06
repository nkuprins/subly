package com.app.subly.controller;

import com.app.subly.dto.dashboard.DashboardSummaryDto;
import com.app.subly.dto.dashboard.UpcomingPaymentDto;
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
    public DashboardSummaryDto getSummary(Principal principal) {
        return dashboardService.getSummary(principal.getName());
    }

    @GetMapping("/upcoming-payments")
    public List<UpcomingPaymentDto> getUpcomingPayments(Principal principal) {
        return dashboardService.getUpcomingPayments(principal.getName());
    }
}
