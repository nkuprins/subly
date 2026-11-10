package com.app.subly.service;

import com.app.subly.dto.admin.AdminStatisticsResponse;
import com.app.subly.dto.admin.PopularServiceResponse;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.util.CurrencyConverter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private static final BigDecimal MONTHS_IN_YEAR = BigDecimal.valueOf(12);

    private final UserRepository userRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionMembershipRepository membershipRepository;

    @Transactional(readOnly = true)
    public AdminStatisticsResponse getStatistics() {
        long totalUsers = userRepository.count();

        long activeSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

        List<Subscription> catalogSubscriptions = subscriptionRepository.findByStatusAndProviderProviderType(
                SubscriptionStatus.ACTIVE, ProviderType.CATALOG);

        List<PopularServiceResponse> popularServices = catalogSubscriptions.stream()
                .collect(Collectors.groupingBy(
                        subscription -> subscription.getProvider().getId()))
                .entrySet()
                .stream()
                .map(entry -> new PopularServiceResponse(
                        entry.getKey(),
                        entry.getValue().get(0).getProvider().getName(),
                        entry.getValue().size()))
                .sorted(Comparator.comparingLong(PopularServiceResponse::activeSubscriptionCount)
                        .reversed()
                        .thenComparing(PopularServiceResponse::providerName))
                .limit(5)
                .toList();

        BigDecimal averageMonthlySpend = calculateAverageMonthlySpend(totalUsers);

        return new AdminStatisticsResponse(
                totalUsers, activeSubscriptions, averageMonthlySpend, Currency.EUR, popularServices);
    }

    private BigDecimal calculateAverageMonthlySpend(long totalUsers) {
        if (totalUsers == 0) {
            return BigDecimal.ZERO.setScale(2);
        }

        List<SubscriptionMembership> memberships =
                membershipRepository.findBySubscriptionStatus(SubscriptionStatus.ACTIVE);

        Map<Long, Long> memberCounts = memberships.stream()
                .collect(Collectors.groupingBy(
                        membership -> membership.getSubscription().getId(), Collectors.counting()));

        BigDecimal yearlySpend = BigDecimal.ZERO;

        for (SubscriptionMembership membership : memberships) {
            Subscription subscription = membership.getSubscription();

            long memberCount = memberCounts.get(subscription.getId());

            BigDecimal personalShare = subscription.shareFor(membership.getRole(), memberCount);

            BigDecimal personalShareEur = CurrencyConverter.toEur(personalShare, subscription.getCurrency());

            BigDecimal paymentsPerYear =
                    BigDecimal.valueOf(subscription.getBillingCycle().getPaymentsPerYear());

            yearlySpend = yearlySpend.add(personalShareEur.multiply(paymentsPerYear));
        }

        return yearlySpend.divide(MONTHS_IN_YEAR.multiply(BigDecimal.valueOf(totalUsers)), 2, RoundingMode.HALF_UP);
    }
}
