package com.app.subly.service;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestClock.TOKYO_TODAY;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestClock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentDateSchedulerTest {

    private static final Instant FIFTEEN_MINUTES_LATER = TestClock.NOW.plus(Duration.ofMinutes(15));
    private static final Instant MIDNIGHT_IN_DEFAULT_ZONE =
            TODAY.plusDays(1).atStartOfDay(DEFAULT_ZONE).toInstant();

    @Mock
    UserRepository userRepository;

    @Mock
    PaymentService paymentService;

    @Mock
    Clock clock;

    Instant now = TestClock.NOW;
    PaymentDateScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentDateScheduler(userRepository, paymentService, clock);
        when(clock.withZone(any(ZoneId.class))).thenAnswer(call -> Clock.fixed(now, call.getArgument(0)));
        when(userRepository.findDistinctTimeZones()).thenReturn(List.of(DEFAULT_ZONE, TOKYO));
    }

    @Test
    void advancePastPaymentDates_shouldUseTheTodayOfEachZone() {
        scheduler.advancePastPaymentDates();

        verify(paymentService).advancePaymentDates(DEFAULT_ZONE, TODAY);
        verify(paymentService).advancePaymentDates(TOKYO, TOKYO_TODAY);
    }

    @Test
    void advancePastPaymentDates_shouldSkipZoneUntilItsDateChanges() {
        scheduler.advancePastPaymentDates();
        now = FIFTEEN_MINUTES_LATER;
        scheduler.advancePastPaymentDates();
        now = MIDNIGHT_IN_DEFAULT_ZONE;
        scheduler.advancePastPaymentDates();

        verify(paymentService, times(1)).advancePaymentDates(DEFAULT_ZONE, TODAY);
        verify(paymentService, times(1)).advancePaymentDates(DEFAULT_ZONE, TODAY.plusDays(1));
        verify(paymentService, times(1)).advancePaymentDates(TOKYO, TOKYO_TODAY);
        verifyNoMoreInteractions(paymentService);
    }
}
