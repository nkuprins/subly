package com.app.subly.service.calculator;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.entity.category.Category;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.Currency;
import com.app.subly.service.share.MemberShare;
import com.app.subly.util.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class DashboardCalculator {

    private static final int UPCOMING_DAYS = 30;
    private static final Comparator<CategorySpendingResponse> BIGGEST_SPENDING_FIRST =
            Comparator.comparing(CategorySpendingResponse::yearlyTotal).reversed();

    public static DashboardSummaryResponse summarize(List<MemberShare> shares, Currency targetCurrency) {
        BigDecimal yearlyTotal =
                shares.stream().map(share -> share.yearlyIn(targetCurrency)).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummaryResponse(
                Money.monthlyFromYearly(yearlyTotal), yearlyTotal, targetCurrency, shares.size());
    }

    public static List<UpcomingPaymentResponse> upcomingPayments(
            List<MemberShare> shares, LocalDate today, Currency targetCurrency) {
        return shares.stream()
                .map(share -> UpcomingPaymentResponse.from(
                        share.subscription(), share.amountIn(targetCurrency), targetCurrency, today))
                .filter(payment -> payment.daysLeft() >= 0 && payment.daysLeft() <= UPCOMING_DAYS)
                .toList();
    }

    public static List<CategorySpendingResponse> spendingByCategory(List<MemberShare> shares, Currency targetCurrency) {
        Map<CategoryKey, BigDecimal> yearlyByCategory = shares.stream()
                .collect(Collectors.toMap(
                        share -> CategoryKey.of(share.subscription()),
                        share -> share.yearlyIn(targetCurrency),
                        BigDecimal::add));
        return yearlyByCategory.entrySet().stream()
                .map(entry -> entry.getKey().toResponse(entry.getValue(), targetCurrency))
                .sorted(BIGGEST_SPENDING_FIRST)
                .toList();
    }

    public static MoneyResponse savings(List<MemberShare> shares, LocalDate today, Currency targetCurrency) {
        BigDecimal total = shares.stream()
                .map(share -> share.savedUntil(today, targetCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new MoneyResponse(total, targetCurrency);
    }

    private record CategoryKey(Long id, String name) {

        static CategoryKey of(Subscription subscription) {
            Category category = subscription.getProvider().getCategory();
            return new CategoryKey(category.getId(), category.getName());
        }

        CategorySpendingResponse toResponse(BigDecimal yearlyTotal, Currency currency) {
            return new CategorySpendingResponse(name, Money.monthlyFromYearly(yearlyTotal), yearlyTotal, currency);
        }
    }
}
