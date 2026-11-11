package com.app.subly.service;

import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.util.CurrencyConverter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SubscriptionShares {

    private final SubscriptionMembershipRepository membershipRepository;

    List<Share> forUser(String username, SubscriptionStatus status) {
        List<SubscriptionMembership> memberships = membershipRepository.findMembershipsOf(username, status);
        if (memberships.isEmpty()) {
            return List.of();
        }

        Map<Long, Long> memberCounts = countMembers(memberships);
        return memberships.stream()
                .map(membership -> shareOf(membership, memberCounts))
                .toList();
    }

    private Map<Long, Long> countMembers(List<SubscriptionMembership> memberships) {
        List<Long> subscriptionIds =
                memberships.stream().map(SubscriptionShares::subscriptionId).toList();
        return membershipRepository.findBySubscriptionIdIn(subscriptionIds).stream()
                .collect(Collectors.groupingBy(SubscriptionShares::subscriptionId, Collectors.counting()));
    }

    private static Share shareOf(SubscriptionMembership membership, Map<Long, Long> memberCounts) {
        Subscription subscription = membership.getSubscription();
        long memberCount = memberCounts.get(subscription.getId());
        return new Share(membership, subscription.shareFor(membership.getRole(), memberCount));
    }

    private static Long subscriptionId(SubscriptionMembership membership) {
        return membership.getSubscription().getId();
    }

    record Share(SubscriptionMembership membership, BigDecimal amount) {

        public Subscription subscription() {
            return membership.getSubscription();
        }

        public BigDecimal savedUntil(LocalDate today) {
            return amount.multiply(BigDecimal.valueOf(subscription().skippedPaymentsUntil(today)));
        }

        public BigDecimal savedUntil(LocalDate today, Currency target) {
            return toCurrency(savedUntil(today), target);
        }

        public BigDecimal amountIn(Currency target) {
            return toCurrency(amount, target);
        }

        private BigDecimal toCurrency(BigDecimal value, Currency target) {
            return CurrencyConverter.convert(value, subscription().getCurrency(), target);
        }
    }
}
