package com.app.subly.service;

import com.app.subly.dto.dashboard.DashboardResponse;
import com.app.subly.enums.Currency;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final SubscriptionShares subscriptionShares;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(String username, Currency targetCurrency) {
        log.debug("Building dashboard for username={} targetCurrency={}", username, targetCurrency);
        LocalDate today = userRepository.getByUsername(username).today(clock);
        List<Share> active = subscriptionShares.forUser(username, SubscriptionStatus.ACTIVE);
        List<Share> cancelled = subscriptionShares.forUser(username, SubscriptionStatus.CANCELLED);
        Map<Boolean, List<Share>> bySharable = active.stream()
                .collect(Collectors.partitioningBy(share -> share.subscription().isSharable()));
        DashboardResponse dashboard = new DashboardResponse(
                DashboardCalculator.summarize(active, targetCurrency),
                DashboardCalculator.summarize(bySharable.get(false), targetCurrency),
                DashboardCalculator.summarize(bySharable.get(true), targetCurrency),
                DashboardCalculator.upcomingPayments(active, today, targetCurrency),
                DashboardCalculator.spendingByCategory(active, targetCurrency),
                DashboardCalculator.savings(cancelled, today, targetCurrency));
        log.debug(
                "Built dashboard for username={} activeShares={} cancelledShares={}",
                username,
                active.size(),
                cancelled.size());
        return dashboard;
    }
}
