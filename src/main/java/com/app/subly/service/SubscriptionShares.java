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

    // How much the user pays for each of their subscriptions with the given status.
    List<Share> load(String username, SubscriptionStatus status) {
        // The subscriptions the user is in, and their role in each one.
        List<SubscriptionMembership> memberships =
                membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        username, status);

        if (memberships.isEmpty()) {
            return List.of();
        }

        List<Long> subscriptionIds = memberships.stream()
                .map(membership -> membership.getSubscription().getId())
                .toList();

        // How many people share each of these subscriptions.
        Map<Long, Long> memberCounts = membershipRepository.findBySubscriptionIdIn(subscriptionIds).stream()
                .collect(Collectors.groupingBy(
                        membership -> membership.getSubscription().getId(), Collectors.counting()));

        return memberships.stream()
                .map(membership -> {
                    Subscription subscription = membership.getSubscription();
                    // Split the price between the people. The owner also pays the leftover cent.
                    long count = memberCounts.getOrDefault(subscription.getId(), 0L);
                    return new Share(membership, subscription.shareFor(membership.getRole(), count));
                })
                .toList();
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
