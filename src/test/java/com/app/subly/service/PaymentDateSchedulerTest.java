package com.app.subly.service;

import com.app.subly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class PaymentDateSchedulerTest {

    private static final ZoneId RIGA = ZoneId.of("Europe/Riga");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    @Mock UserRepository userRepository;
    @Mock SubscriptionService subscriptionService;

    // Starts at 16:00 UTC on 4 Nov: still 4 Nov in Riga, but already 5 Nov in Tokyo.
    final MovableClock clock = new MovableClock(Instant.parse("2026-11-04T16:00:00Z"));
    PaymentDateScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentDateScheduler(userRepository, subscriptionService, clock);
        given(userRepository.findDistinctTimeZones()).willReturn(List.of(RIGA, TOKYO));
    }

    @Test
    void advancesEachZoneWithThatZonesToday() {
        scheduler.advancePastPaymentDates();

        then(subscriptionService).should().advancePaymentDates(RIGA, LocalDate.of(2026, 11, 4));
        then(subscriptionService).should().advancePaymentDates(TOKYO, LocalDate.of(2026, 11, 5));
    }

    @Test
    void skipsZoneUntilItsDateChanges() {
        scheduler.advancePastPaymentDates();
        clock.moveTo("2026-11-04T16:15:00Z");
        scheduler.advancePastPaymentDates();
        // 22:00 UTC on 4 Nov is midnight of 5 Nov in Riga; Tokyo is still on 5 Nov.
        clock.moveTo("2026-11-04T22:00:00Z");
        scheduler.advancePastPaymentDates();

        then(subscriptionService).should(times(1))
                .advancePaymentDates(RIGA, LocalDate.of(2026, 11, 4));
        then(subscriptionService).should(times(1))
                .advancePaymentDates(RIGA, LocalDate.of(2026, 11, 5));
        then(subscriptionService).should(times(1))
                .advancePaymentDates(TOKYO, LocalDate.of(2026, 11, 5));
        then(subscriptionService).shouldHaveNoMoreInteractions();
    }

    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void moveTo(String instant) {
            now = Instant.parse(instant);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
