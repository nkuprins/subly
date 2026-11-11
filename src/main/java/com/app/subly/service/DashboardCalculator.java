package com.app.subly.service;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.category.Category;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.service.SubscriptionShares.Share;
import com.app.subly.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class DashboardCalculator {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final int UPCOMING_DAYS = 30;
    private static final Comparator<CategorySpendingResponse> BIGGEST_SPENDING_FIRST =
            Comparator.comparing(CategorySpendingResponse::yearlyTotal).reversed();

    static DashboardSummaryResponse summarize(List<Share> shares, Currency targetCurrency) {
        BigDecimal yearlyTotal = shares.stream()
                .map(share -> yearlyCost(share, targetCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummaryResponse(monthly(yearlyTotal), yearlyTotal, targetCurrency, shares.size());
    }

    static List<UpcomingPaymentResponse> upcomingPayments(
            List<Share> shares, LocalDate today, Currency targetCurrency) {
        return shares.stream()
                .map(share -> UpcomingPaymentResponse.from(
                        share.subscription(), share.amountIn(targetCurrency), targetCurrency, today))
                .filter(payment -> payment.daysLeft() >= 0 && payment.daysLeft() <= UPCOMING_DAYS)
                .toList();
    }

    static List<CategorySpendingResponse> spendingByCategory(List<Share> shares, Currency targetCurrency) {
        Map<CategoryKey, BigDecimal> yearlyByCategory = shares.stream()
                .collect(Collectors.toMap(
                        share -> CategoryKey.of(share.subscription()),
                        share -> yearlyCost(share, targetCurrency),
                        BigDecimal::add));
        return yearlyByCategory.entrySet().stream()
                .map(entry -> entry.getKey().toResponse(entry.getValue(), targetCurrency))
                .sorted(BIGGEST_SPENDING_FIRST)
                .toList();
    }

    static MoneyResponse savings(List<Share> shares, LocalDate today, Currency targetCurrency) {
        BigDecimal total = shares.stream()
                .map(share -> share.savedUntil(today, targetCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MoneyResponse(total, targetCurrency);
    }

    private static BigDecimal yearlyCost(Share share, Currency targetCurrency) {
        int paymentsPerYear = share.subscription().getBillingCycle().getPaymentsPerYear();
        return share.amountIn(targetCurrency).multiply(BigDecimal.valueOf(paymentsPerYear));
    }

    private static BigDecimal monthly(BigDecimal yearly) {
        return yearly.divide(MONTHS_IN_YEAR, Money.FRACTION_DIGITS, RoundingMode.HALF_UP);
    }

    private record CategoryKey(Long id, String name) {

        static CategoryKey of(Subscription subscription) {
            Category category = subscription.getProvider().getCategory();
            return new CategoryKey(category.getId(), category.getName());
        }

        CategorySpendingResponse toResponse(BigDecimal yearlyTotal, Currency currency) {
            return new CategorySpendingResponse(id, name, monthly(yearlyTotal), yearlyTotal, currency);
        }
    }
}
