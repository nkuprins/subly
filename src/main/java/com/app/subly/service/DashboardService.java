package com.app.subly.service;

import com.app.subly.dto.dashboard.DashboardSummaryDto;
import com.app.subly.dto.dashboard.UpcomingPaymentDto;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    private final SubscriptionMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardSummaryDto getSummary(String username) {

        List<SubscriptionShare> shares = loadSubscriptionShares(username);

        BigDecimal monthlyCost = BigDecimal.ZERO;
        BigDecimal yearlyCost = BigDecimal.ZERO;

        for (SubscriptionShare share : shares) {

            Subscription subscription = share.subscription();

            BigDecimal amountEur = CurrencyConverter.toEur(share.amount(), subscription.getCurrency());

            if (subscription.getBillingCycle() == BillingCycle.MONTHLY) {
                monthlyCost = monthlyCost.add(amountEur);
            } else if (subscription.getBillingCycle() == BillingCycle.YEARLY) {
                yearlyCost = yearlyCost.add(amountEur);
            }
        }

        BigDecimal yearlyTotal = monthlyCost.multiply(MONTHS_IN_YEAR).add(yearlyCost);

        BigDecimal monthlyTotal = yearlyTotal.divide(MONTHS_IN_YEAR, 2, RoundingMode.HALF_UP);

        return new DashboardSummaryDto(monthlyTotal, yearlyTotal.setScale(2, RoundingMode.HALF_UP), Currency.EUR);
    }

    @Transactional(readOnly = true)
    public List<UpcomingPaymentDto> getUpcomingPayments(String username) {

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

                    return new UpcomingPaymentDto(
                            subscription.getId(),
                            subscription.getProvider().getName(),
                            share.amount(),
                            subscription.getCurrency(),
                            subscription.getNextPaymentDate(),
                            ChronoUnit.DAYS.between(today, subscription.getNextPaymentDate()));
                })
                .toList();
    }

    private List<SubscriptionShare> loadSubscriptionShares(String username) {
        List<SubscriptionMembership> memberships =
                membershipRepository.findByUser_UsernameAndSubscription_StatusOrderBySubscription_NextPaymentDateAsc(
                        username, SubscriptionStatus.ACTIVE);

        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> subscriptionIds = memberships.stream()
                .map(membership -> membership.getSubscription().getId())
                .distinct()
                .toList();

        Map<Long, Long> memberCounts = membershipRepository.findBySubscription_IdIn(subscriptionIds).stream()
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

        BigDecimal baseShare = price.divide(BigDecimal.valueOf(memberCount), 2, RoundingMode.DOWN);

        if (membership.getRole() == MembershipRole.OWNER) {
            BigDecimal otherMembersTotal = baseShare.multiply(BigDecimal.valueOf(memberCount - 1));

            return price.subtract(otherMembersTotal);
        }

        return baseShare;
    }

    private record SubscriptionShare(Subscription subscription, BigDecimal amount) {}
}
