package com.app.subly.dto.admin;

import com.app.subly.enums.Currency;
import java.math.BigDecimal;
import java.util.List;

public record AdminStatisticsResponse(
        long totalUsers,
        long activeSubscriptions,
        BigDecimal averageMonthlySpendPerUser,
        Currency currency,
        List<PopularServiceResponse> popularServices) {}
