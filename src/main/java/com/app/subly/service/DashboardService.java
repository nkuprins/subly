package com.app.subly.service;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.category.Category;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.UserRepository;
import com.app.subly.service.SubscriptionShares.Share;
import com.app.subly.util.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);
    private static final int UPCOMING_DAYS = 30;
    private static final Comparator<CategorySpendingResponse> BIGGEST_SPENDING_FIRST =
            Comparator.comparing(CategorySpendingResponse::yearlyTotal).reversed();

    private final SubscriptionShares subscriptionShares;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String username, Currency targetCurrency) {
        LocalDate today = userRepository.getByUsername(username).today(clock);
        List<Share> active = subscriptionShares.load(username, SubscriptionStatus.ACTIVE);
        List<Share> cancelled = subscriptionShares.load(username, SubscriptionStatus.CANCELLED);
        Map<Boolean, List<Share>> bySharable = active.stream()
                .collect(Collectors.partitioningBy(share -> share.subscription().isSharable()));
        return new DashboardResponse(
                summarize(active, targetCurrency),
                summarize(bySharable.get(false), targetCurrency),
                summarize(bySharable.get(true), targetCurrency),
                upcomingPayments(active, today, targetCurrency),
                spendingByCategory(active, targetCurrency),
                savings(cancelled, today, targetCurrency));
    }

    private static DashboardSummaryResponse summarize(List<Share> shares, Currency targetCurrency) {
        BigDecimal yearlyTotal = shares.stream()
                .map(share -> yearlyCost(share, targetCurrency))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new DashboardSummaryResponse(monthly(yearlyTotal), yearlyTotal, targetCurrency, shares.size());
    }

    private static List<UpcomingPaymentResponse> upcomingPayments(
            List<Share> shares, LocalDate today, Currency targetCurrency) {
        return shares.stream()
                .map(share -> UpcomingPaymentResponse.from(
                        share.subscription(), share.amountIn(targetCurrency), targetCurrency, today))
                .filter(payment -> payment.daysLeft() >= 0 && payment.daysLeft() <= UPCOMING_DAYS)
                .toList();
    }

    private static List<CategorySpendingResponse> spendingByCategory(List<Share> shares, Currency targetCurrency) {
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

    private static MoneyResponse savings(List<Share> shares, LocalDate today, Currency targetCurrency) {
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
