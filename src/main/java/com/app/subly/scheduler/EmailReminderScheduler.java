package com.app.subly.scheduler;

import com.app.subly.service.SubscriptionReminderService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailReminderScheduler {
    private final SubscriptionReminderService service;

    // Starts working each hour, after 10 seconds the spring application starts.
    @Scheduled(initialDelay = 10, fixedRate = 3600, timeUnit = TimeUnit.SECONDS)
    public void runEmailSendingLoop() {
        log.info("RUNNING EMAIL SENDING LOOP");
        this.service.runEmailSending();
        log.info("EMAIL SENDING LOOP HAS BEEN FINISHED");
    }
}
