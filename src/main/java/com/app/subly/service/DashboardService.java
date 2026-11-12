package com.app.subly.service;

import com.app.subly.dto.dashboard.DashboardResponse;
import com.app.subly.enums.Currency;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.UserRepository;
import com.app.subly.util.DashboardCalculator;
import com.app.subly.util.MemberShare;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SubscriptionShares subscriptionShares;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String username, Currency targetCurrency) {
        LocalDate today = userRepository.getByUsername(username).today(clock);
        List<MemberShare> active = subscriptionShares.forUser(username, SubscriptionStatus.ACTIVE);
        List<MemberShare> cancelled = subscriptionShares.forUser(username, SubscriptionStatus.CANCELLED);
        Map<Boolean, List<MemberShare>> bySharable = active.stream()
                .collect(Collectors.partitioningBy(share -> share.subscription().isSharable()));
        return new DashboardResponse(
                DashboardCalculator.summarize(active, targetCurrency),
                DashboardCalculator.summarize(bySharable.get(false), targetCurrency),
                DashboardCalculator.summarize(bySharable.get(true), targetCurrency),
                DashboardCalculator.upcomingPayments(active, today, targetCurrency),
                DashboardCalculator.spendingByCategory(active, targetCurrency),
                DashboardCalculator.savings(cancelled, today, targetCurrency));
    }
}
