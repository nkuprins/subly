package com.app.subly.dto.dashboard;

import com.app.subly.dto.MoneyResponse;
import java.util.List;

public record DashboardResponse(
        DashboardSummaryResponse summary,
        DashboardSummaryResponse individualSummary,
        DashboardSummaryResponse sharedSummary,
        List<UpcomingPaymentResponse> upcoming,
        List<CategorySpendingResponse> categories,
        MoneyResponse savings) {}
