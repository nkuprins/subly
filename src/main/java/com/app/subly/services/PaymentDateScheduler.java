package com.app.subly.services;

import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class PaymentDateScheduler {

    private final SubscriptionMembershipRepository membershipRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    // every 15 minutes
    @Scheduled(cron = "0 */15 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void advancePastPaymentDates() {
        for (ZoneId zone : userRepository.findDistinctTimeZones()) {
            LocalDate today = LocalDate.now(clock.withZone(zone));
            membershipRepository
                    .findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                            MembershipRole.OWNER, SubscriptionStatus.ACTIVE, zone, today)
                    .forEach(owner -> owner.getSubscription().advancePaymentDate(today));
        }
    }
}
