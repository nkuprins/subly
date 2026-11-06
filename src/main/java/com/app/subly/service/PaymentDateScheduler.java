package com.app.subly.service;

import com.app.subly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PaymentDateScheduler {

    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;
    private final Clock clock;
    private final Map<ZoneId, LocalDate> processedDates = new ConcurrentHashMap<>();

    // every 15 minutes
    @Scheduled(cron = "0 */15 * * * *")
    @EventListener(ApplicationReadyEvent.class)
    public void advancePastPaymentDates() {
        for (ZoneId zone : userRepository.findDistinctTimeZones()) {
            LocalDate today = LocalDate.now(clock.withZone(zone));
            if (!today.equals(processedDates.get(zone))) {
                subscriptionService.advancePaymentDates(zone, today);
                processedDates.put(zone, today);
            }
        }
    }
}
