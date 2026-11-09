package com.app.subly.service;

import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.category.Category;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.UserRepository;
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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);

    private static final int UPCOMING_DAYS = 30;

    private static final int MONEY_SCALE = 2;

    private final SubscriptionMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(String username) {

        List<SubscriptionShare> shares = loadSubscriptionShares(username);

        BigDecimal yearlyTotal = BigDecimal.ZERO;

        for (SubscriptionShare share : shares) {

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

        return loadSubscriptionShares(username).stream()
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

        for (SubscriptionShare share : loadSubscriptionShares(username)) {
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

    private List<SubscriptionShare> loadSubscriptionShares(String username) {
        List<SubscriptionMembership> memberships =
                membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        username, SubscriptionStatus.ACTIVE);

        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> subscriptionIds = memberships.stream()
                .map(membership -> membership.getSubscription().getId())
                .distinct()
                .toList();

        Map<Long, Long> memberCounts = membershipRepository.findBySubscriptionIdIn(subscriptionIds).stream()
                .collect(Collectors.groupingBy(
                        membership -> membership.getSubscription().getId(), Collectors.counting()));

        return memberships.stream()
                .map(membership -> {
                    Subscription subscription = membership.getSubscription();

                    long count = memberCounts.getOrDefault(subscription.getId(), 0L);

                    BigDecimal amount = calculateShare(membership, count);

                    return new SubscriptionShare(subscription, amount);
                })
                .toList();
    }

    private BigDecimal calculateShare(SubscriptionMembership membership, long memberCount) {
        if (memberCount <= 0) {
            throw new IllegalStateException("Subscription has no members");
        }

        BigDecimal price = membership.getSubscription().getPrice();

        BigDecimal baseShare = price.divide(BigDecimal.valueOf(memberCount), MONEY_SCALE, RoundingMode.DOWN);

        if (membership.getRole() == MembershipRole.OWNER) {
            BigDecimal otherMembersTotal = baseShare.multiply(BigDecimal.valueOf(memberCount - 1));

            return price.subtract(otherMembersTotal);
        }

        return baseShare;
    }

    private record SubscriptionShare(Subscription subscription, BigDecimal amount) {}

    private record CategoryKey(Long id, String name) {}
}
