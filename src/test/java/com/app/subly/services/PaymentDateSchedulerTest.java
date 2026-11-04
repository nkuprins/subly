package com.app.subly.services;

import com.app.subly.models.User;
import com.app.subly.models.subscription.BillingCycle;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.ServiceProvider;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.models.subscription.SubscriptionMembership;
import com.app.subly.models.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class PaymentDateSchedulerTest {

    // CLOCK is 16:00 UTC on 4 Nov: still 4 Nov in Riga, but already 5 Nov in Tokyo.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T16:00:00Z"), ZoneOffset.UTC);
    private static final ZoneId RIGA = ZoneId.of("Europe/Riga");
    private static final ZoneId TOKYO = ZoneId.of("Asia/Tokyo");

    @Mock SubscriptionMembershipRepository membershipRepository;
    @Mock UserRepository userRepository;

    PaymentDateScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PaymentDateScheduler(membershipRepository, userRepository, CLOCK);
    }

    @Test
    void asksForEachZoneWithThatZonesToday() {
        given(userRepository.findDistinctTimeZones()).willReturn(List.of(RIGA, TOKYO));

        scheduler.advancePastPaymentDates();

        then(membershipRepository).should().findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                MembershipRole.OWNER, SubscriptionStatus.ACTIVE, RIGA, LocalDate.of(2026, 11, 4));
        then(membershipRepository).should().findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                MembershipRole.OWNER, SubscriptionStatus.ACTIVE, TOKYO, LocalDate.of(2026, 11, 5));
    }

    @Test
    void movesReturnedDatesToNextCycleFromThatZonesToday() {
        Subscription tokyo = subscription(LocalDate.of(2026, 11, 4));
        given(userRepository.findDistinctTimeZones()).willReturn(List.of(TOKYO));
        given(membershipRepository.findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                MembershipRole.OWNER, SubscriptionStatus.ACTIVE, TOKYO, LocalDate.of(2026, 11, 5)))
                .willReturn(List.of(SubscriptionMembership.owner(new User(), tokyo)));

        scheduler.advancePastPaymentDates();

        assertThat(tokyo.getNextPaymentDate()).isEqualTo(LocalDate.of(2026, 12, 4));
    }

    private static Subscription subscription(LocalDate firstPayment) {
        return new Subscription(ServiceProvider.catalog("Netflix", new Category("Streaming")),
                new BigDecimal("9.99"), BillingCycle.MONTHLY, firstPayment, false);
    }
}
