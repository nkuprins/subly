package com.app.subly.service;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestClock.TOKYO_TODAY;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestClock;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentDateSchedulerTest {

    private static final Instant FIFTEEN_MINUTES_LATER = Instant.parse("2026-11-04T16:15:00Z");
    private static final Instant MIDNIGHT_IN_DEFAULT_ZONE = Instant.parse("2026-11-04T22:00:00Z");

    @Mock
    UserRepository userRepository;

    @Mock
    SubscriptionService subscriptionService;

    final MovableClock clock = new MovableClock(TestClock.NOW);
    PaymentDateScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentDateScheduler(userRepository, subscriptionService, clock);
        when(userRepository.findDistinctTimeZones()).thenReturn(List.of(DEFAULT_ZONE, TOKYO));
    }

    @Test
    void advancePastPaymentDates_shouldUseTheTodayOfEachZone() {
        scheduler.advancePastPaymentDates();

        verify(subscriptionService).advancePaymentDates(DEFAULT_ZONE, TODAY);
        verify(subscriptionService).advancePaymentDates(TOKYO, TOKYO_TODAY);
    }

    @Test
    void advancePastPaymentDates_shouldSkipZoneUntilItsDateChanges() {
        scheduler.advancePastPaymentDates();
        clock.moveTo(FIFTEEN_MINUTES_LATER);
        scheduler.advancePastPaymentDates();
        clock.moveTo(MIDNIGHT_IN_DEFAULT_ZONE);
        scheduler.advancePastPaymentDates();

        verify(subscriptionService, times(1)).advancePaymentDates(DEFAULT_ZONE, TODAY);
        verify(subscriptionService, times(1)).advancePaymentDates(DEFAULT_ZONE, TODAY.plusDays(1));
        verify(subscriptionService, times(1)).advancePaymentDates(TOKYO, TOKYO_TODAY);
        verifyNoMoreInteractions(subscriptionService);
    }

    private static final class MovableClock extends Clock {

        private Instant now;

        MovableClock(Instant now) {
            this.now = now;
        }

        void moveTo(Instant instant) {
            now = instant;
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
