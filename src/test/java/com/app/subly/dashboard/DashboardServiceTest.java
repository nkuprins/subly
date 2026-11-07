package com.app.subly.dashboard;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.subly.dto.dashboard.DashboardSummaryDto;
import com.app.subly.dto.dashboard.UpcomingPaymentDto;
import com.app.subly.model.TestUsers;
import com.app.subly.model.User;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionProvider;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.service.DashboardService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private SubscriptionMembershipRepository repository;

    @Mock
    private UserRepository userRepository;

    private DashboardService dashboardService;

    private static final String USERNAME = "alice";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-04T16:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(repository, userRepository, CLOCK);
    }

    @Test
    void returnsZeroWhenUserHasNoSubscriptions() {
        when(repository.findByUser_UsernameAndSubscription_StatusOrderBySubscription_NextPaymentDateAsc(
                        USERNAME, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        DashboardSummaryDto result = dashboardService.getSummary(USERNAME);

        assertMoney("0.00", result.monthlyTotal());
        assertMoney("0.00", result.yearlyTotal());
        assertEquals(Currency.EUR, result.currency());
        verify(repository, never()).findBySubscription_IdIn(anyCollection());
    }

    @Test
    void calculatesMonthlyAndYearlyTotalsWithCurrencyConversion() {
        Subscription netflix =
                subscription(1L, "Netflix", "12.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.plusDays(5), false);
        Subscription youtube =
                subscription(2L, "YouTube", "10.00", BillingCycle.MONTHLY, Currency.USD, TODAY.plusDays(6), false);
        Subscription gym =
                subscription(3L, "Gym", "100.00", BillingCycle.YEARLY, Currency.GBP, TODAY.plusDays(7), false);

        SubscriptionMembership first = membership("alice", netflix, MembershipRole.OWNER);
        SubscriptionMembership second = membership("alice", youtube, MembershipRole.OWNER);
        SubscriptionMembership third = membership("alice", gym, MembershipRole.OWNER);
        stubMemberships(List.of(first, second, third), List.of(first, second, third));

        DashboardSummaryDto result = dashboardService.getSummary(USERNAME);

        assertMoney("30.90", result.monthlyTotal());
        assertMoney("370.80", result.yearlyTotal());
        assertEquals(Currency.EUR, result.currency());
    }

    @Test
    void includesFreeTrialAtItsFuturePrice() {
        Subscription trial =
                subscription(4L, "Trial", "20.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.plusDays(10), true);
        SubscriptionMembership owner = membership("alice", trial, MembershipRole.OWNER);
        stubMemberships(List.of(owner), List.of(owner));

        DashboardSummaryDto result = dashboardService.getSummary(USERNAME);

        assertMoney("20.00", result.monthlyTotal());
        assertMoney("240.00", result.yearlyTotal());
    }

    @Test
    void roundsOnlyFinalMonthlyAverageForYearlySubscription() {
        Subscription yearly =
                subscription(5L, "Yearly", "100.00", BillingCycle.YEARLY, Currency.EUR, TODAY.plusDays(10), false);
        SubscriptionMembership owner = membership("alice", yearly, MembershipRole.OWNER);
        stubMemberships(List.of(owner), List.of(owner));

        DashboardSummaryDto result = dashboardService.getSummary(USERNAME);

        assertMoney("8.33", result.monthlyTotal());
        assertMoney("100.00", result.yearlyTotal());
    }

    @Test
    void ownerGetsRemainderCentWhenPriceIsShared() {
        Subscription shared =
                subscription(6L, "Shared", "10.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.plusDays(7), false);
        SubscriptionMembership owner = membership("alice", shared, MembershipRole.OWNER);
        SubscriptionMembership bob = membership("bob", shared, MembershipRole.MEMBER);
        SubscriptionMembership carol = membership("carol", shared, MembershipRole.MEMBER);
        stubMemberships(List.of(owner), List.of(owner, bob, carol));

        DashboardSummaryDto result = dashboardService.getSummary(USERNAME);

        assertMoney("3.34", result.monthlyTotal());
        assertMoney("40.08", result.yearlyTotal());
    }

    @Test
    void regularMemberDoesNotGetRemainderCent() {
        Subscription shared =
                subscription(7L, "Shared", "10.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.plusDays(7), false);
        SubscriptionMembership owner = membership("bob", shared, MembershipRole.OWNER);
        SubscriptionMembership member = membership("alice", shared, MembershipRole.MEMBER);
        SubscriptionMembership third = membership("carol", shared, MembershipRole.MEMBER);
        stubMemberships(List.of(member), List.of(owner, member, third));

        DashboardSummaryDto result = dashboardService.getSummary(USERNAME);

        assertMoney("3.33", result.monthlyTotal());
        assertMoney("39.96", result.yearlyTotal());
    }

    @Test
    void requestsOnlyActiveSubscriptionsFromRepository() {
        when(repository.findByUser_UsernameAndSubscription_StatusOrderBySubscription_NextPaymentDateAsc(
                        USERNAME, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of());

        dashboardService.getSummary(USERNAME);

        verify(repository)
                .findByUser_UsernameAndSubscription_StatusOrderBySubscription_NextPaymentDateAsc(
                        USERNAME, SubscriptionStatus.ACTIVE);
    }

    @Test
    void upcomingPaymentsIncludeTodayAndDayThirtyButNotPastOrDayThirtyOne() {
        Subscription past =
                subscription(8L, "Past", "5.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.minusDays(1), false);
        Subscription todaySub = subscription(9L, "Today", "6.00", BillingCycle.MONTHLY, Currency.EUR, TODAY, false);
        Subscription inThirty =
                subscription(10L, "Thirty", "7.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.plusDays(30), false);
        Subscription afterWindow =
                subscription(11L, "Later", "8.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.plusDays(31), false);
        List<SubscriptionMembership> mine = List.of(
                membership("alice", inThirty, MembershipRole.OWNER),
                membership("alice", afterWindow, MembershipRole.OWNER),
                membership("alice", past, MembershipRole.OWNER),
                membership("alice", todaySub, MembershipRole.OWNER));
        stubMemberships(mine, mine);
        givenUserIn(User.DEFAULT_TIME_ZONE);

        List<UpcomingPaymentDto> result = dashboardService.getUpcomingPayments(USERNAME);

        assertEquals(2, result.size());
        assertEquals("Today", result.get(0).providerName());
        assertEquals(TODAY, result.get(0).nextPaymentDate());
        assertEquals("Thirty", result.get(1).providerName());
        assertEquals(TODAY.plusDays(30), result.get(1).nextPaymentDate());
    }

    @Test
    void upcomingPaymentKeepsOriginalCurrencyAndUsersShare() {
        Subscription shared =
                subscription(12L, "British TV", "10.00", BillingCycle.MONTHLY, Currency.GBP, TODAY.plusDays(2), false);
        SubscriptionMembership owner = membership("bob", shared, MembershipRole.OWNER);
        SubscriptionMembership member = membership("alice", shared, MembershipRole.MEMBER);
        stubMemberships(List.of(member), List.of(owner, member));
        givenUserIn(User.DEFAULT_TIME_ZONE);

        List<UpcomingPaymentDto> result = dashboardService.getUpcomingPayments(USERNAME);

        assertEquals(1, result.size());
        assertEquals(12L, result.get(0).subscriptionId());
        assertMoney("5.00", result.get(0).amount());
        assertEquals(Currency.GBP, result.get(0).currency());
    }

    @Test
    void rejectsInconsistentMembershipCount() {
        Subscription sub =
                subscription(13L, "Invalid", "10.00", BillingCycle.MONTHLY, Currency.EUR, TODAY.plusDays(5), false);
        SubscriptionMembership owner = membership("alice", sub, MembershipRole.OWNER);
        stubMemberships(List.of(owner), List.of());

        assertThrows(IllegalStateException.class, () -> dashboardService.getSummary(USERNAME));
    }

    @Test
    void upcomingPaymentsStartFromUsersToday() {
        // CLOCK is 16:00 UTC on 4 Nov: still 4 Nov in Riga (TODAY), but already 5 Nov in Tokyo.
        // For a Tokyo user the 4 Nov payment is in the past, so only the 5 Nov payment is shown.
        LocalDate tokyoToday = TODAY.plusDays(1);
        givenUserIn(ZoneId.of("Asia/Tokyo"));
        givenMySubscriptionsDueOn(TODAY, tokyoToday);

        List<LocalDate> shown = dashboardService.getUpcomingPayments(USERNAME).stream()
                .map(UpcomingPaymentDto::nextPaymentDate)
                .toList();

        assertEquals(List.of(tokyoToday), shown);
    }

    private void givenMySubscriptionsDueOn(LocalDate... dates) {
        List<SubscriptionMembership> mine = new ArrayList<>();
        long id = 1;
        for (LocalDate date : dates) {
            Subscription subscription =
                    subscription(id++, "Provider " + date, "5.00", BillingCycle.MONTHLY, Currency.EUR, date, false);
            mine.add(membership(USERNAME, subscription, MembershipRole.OWNER));
        }
        stubMemberships(mine, mine);
    }

    private void givenUserIn(ZoneId zone) {
        User user = TestUsers.user(USERNAME);
        user.setTimeZone(zone);
        when(userRepository.getByUsername(USERNAME)).thenReturn(user);
    }

    private void stubMemberships(List<SubscriptionMembership> mine, List<SubscriptionMembership> all) {
        when(repository.findByUser_UsernameAndSubscription_StatusOrderBySubscription_NextPaymentDateAsc(
                        USERNAME, SubscriptionStatus.ACTIVE))
                .thenReturn(mine);
        if (!mine.isEmpty()) {
            when(repository.findBySubscription_IdIn(anyCollection())).thenReturn(all);
        }
    }

    private static Subscription subscription(
            Long id,
            String name,
            String price,
            BillingCycle cycle,
            Currency currency,
            LocalDate nextPayment,
            boolean trial) {
        SubscriptionProvider provider = SubscriptionProvider.catalog(name, new Category("Entertainment"));
        Subscription subscription = new Subscription(provider, new BigDecimal(price), cycle, nextPayment, trial);
        subscription.setCurrency(currency);
        subscription.setId(id);
        return subscription;
    }

    private static SubscriptionMembership membership(String username, Subscription subscription, MembershipRole role) {
        User user = TestUsers.user(username);
        return new SubscriptionMembership(user, subscription, role);
    }

    private static void assertMoney(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }
}
