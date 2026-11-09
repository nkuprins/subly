package com.app.subly.service;

import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.category.Category;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.UserRepository;
import com.app.subly.service.SubscriptionShares.Share;
import com.app.subly.util.CurrencyConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);

    private static final int UPCOMING_DAYS = 30;

    private static final int MONEY_SCALE = 2;

    private final SubscriptionShares subscriptionShares;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(String username) {

        List<Share> shares = subscriptionShares.load(username, SubscriptionStatus.ACTIVE);

        BigDecimal yearlyTotal = BigDecimal.ZERO;

        for (Share share : shares) {

            Subscription subscription = share.subscription();

            BigDecimal amountEur = CurrencyConverter.toEur(share.amount(), subscription.getCurrency());

            BigDecimal paymentsPerYear =
                    BigDecimal.valueOf(subscription.getBillingCycle().getPaymentsPerYear());

            yearlyTotal = yearlyTotal.add(amountEur.multiply(paymentsPerYear));
        }

        BigDecimal monthlyTotal = yearlyTotal.divide(MONTHS_IN_YEAR, MONEY_SCALE, RoundingMode.HALF_UP);

        return new DashboardSummaryResponse(
                monthlyTotal, yearlyTotal.setScale(MONEY_SCALE, RoundingMode.HALF_UP), Currency.EUR);
    }

    @Transactional(readOnly = true)
    public List<UpcomingPaymentResponse> getUpcomingPayments(String username) {

        LocalDate today = userRepository.getByUsername(username).today(clock);
        LocalDate endDate = today.plusDays(UPCOMING_DAYS);

        return subscriptionShares.load(username, SubscriptionStatus.ACTIVE).stream()
                .filter(share -> {
                    LocalDate nextDate = share.subscription().getNextPaymentDate();

                    return !nextDate.isBefore(today) && !nextDate.isAfter(endDate);
                })
                .sorted(Comparator.comparing(share -> share.subscription().getNextPaymentDate()))
                .map(share -> {
                    Subscription subscription = share.subscription();

                    return new UpcomingPaymentResponse(
                            subscription.getId(),
                            subscription.getProvider().getName(),
                            share.amount(),
                            subscription.getCurrency(),
                            subscription.getNextPaymentDate(),
                            ChronoUnit.DAYS.between(today, subscription.getNextPaymentDate()));
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategorySpendingResponse> getSpendingByCategory(String username) {

        Map<CategoryKey, BigDecimal> yearlyByCategory = new HashMap<>();

        for (Share share : subscriptionShares.load(username, SubscriptionStatus.ACTIVE)) {
            Subscription subscription = share.subscription();
            Category category = subscription.getProvider().getCategory();

            CategoryKey key = new CategoryKey(category.getId(), category.getName());

            BigDecimal amountEur = CurrencyConverter.toEur(share.amount(), subscription.getCurrency());

            BigDecimal paymentsPerYear =
                    BigDecimal.valueOf(subscription.getBillingCycle().getPaymentsPerYear());

            BigDecimal yearlyAmount = amountEur.multiply(paymentsPerYear);

            yearlyByCategory.merge(key, yearlyAmount, BigDecimal::add);
        }

        return yearlyByCategory.entrySet().stream()
                .map(entry -> new CategorySpendingResponse(
                        entry.getKey().id(),
                        entry.getKey().name(),
                        entry.getValue().divide(MONTHS_IN_YEAR, MONEY_SCALE, RoundingMode.HALF_UP),
                        entry.getValue().setScale(MONEY_SCALE, RoundingMode.HALF_UP),
                        Currency.EUR))
                .sorted(Comparator.comparing(CategorySpendingResponse::yearlyTotal)
                        .reversed())
                .toList();
    }

    private record CategoryKey(Long id, String name) {}
}
