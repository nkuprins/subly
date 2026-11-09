package com.app.subly.service;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestClock.TOKYO_TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.CAROL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.model.user.User;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestClock;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestSubscriptions;
import com.app.subly.testdata.TestUsers;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @Mock
    UserRepository userRepository;

    DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(membershipRepository, userRepository, TestClock.CLOCK);
    }

    @Test
    void getSummary_shouldReturnZeroWhenUserHasNoSubscriptions() {
        stubMyMemberships(List.of());

        DashboardSummaryResponse result = dashboardService.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("0.00");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("0.00");
        assertThat(result.currency()).isEqualTo(Currency.EUR);
        verify(membershipRepository, never()).findBySubscriptionIdIn(anyCollection());
    }

    @Test
    void getSummary_shouldConvertCurrenciesIntoMonthlyAndYearlyTotals() {
        SubscriptionMembership netflix =
                membership(ALICE, subscription(1L, "Netflix").withPrice("12.00").build(), MembershipRole.OWNER);
        SubscriptionMembership youtube = membership(
                ALICE,
                subscription(2L, "YouTube")
                        .withPrice("10.00")
                        .withCurrency(Currency.USD)
                        .build(),
                MembershipRole.OWNER);
        SubscriptionMembership gym = membership(
                ALICE,
                subscription(3L, "Gym")
                        .withPrice("100.00")
                        .withBillingCycle(BillingCycle.YEARLY)
                        .withCurrency(Currency.GBP)
                        .build(),
                MembershipRole.OWNER);
        stubMemberships(List.of(netflix, youtube, gym), List.of(netflix, youtube, gym));

        DashboardSummaryResponse result = dashboardService.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("30.90");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("370.80");
        assertThat(result.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void getSummary_shouldIncludeFreeTrialAtItsFuturePrice() {
        SubscriptionMembership trial = membership(
                ALICE,
                subscription(4L, "Trial").withPrice("20.00").withFreeTrial().build(),
                MembershipRole.OWNER);
        stubMemberships(List.of(trial), List.of(trial));

        DashboardSummaryResponse result = dashboardService.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("20.00");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("240.00");
    }

    @Test
    void getSummary_shouldRoundOnlyTheFinalMonthlyAverageOfYearlySubscription() {
        SubscriptionMembership yearly = membership(
                ALICE,
                subscription(5L, "Yearly")
                        .withPrice("100.00")
                        .withBillingCycle(BillingCycle.YEARLY)
                        .build(),
                MembershipRole.OWNER);
        stubMemberships(List.of(yearly), List.of(yearly));

        DashboardSummaryResponse result = dashboardService.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("8.33");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void getSummary_shouldGiveOwnerTheRemainderCentOfSharedPrice() {
        Subscription shared = subscription(6L, "Shared").withPrice("10.00").build();
        SubscriptionMembership owner = membership(ALICE, shared, MembershipRole.OWNER);
        SubscriptionMembership bob = membership(BOB, shared, MembershipRole.MEMBER);
        SubscriptionMembership carol = membership(CAROL, shared, MembershipRole.MEMBER);
        stubMemberships(List.of(owner), List.of(owner, bob, carol));

        DashboardSummaryResponse result = dashboardService.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("3.34");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("40.08");
    }

    @Test
    void getSummary_shouldNotGiveRemainderCentToRegularMember() {
        Subscription shared = subscription(7L, "Shared").withPrice("10.00").build();
        SubscriptionMembership owner = membership(BOB, shared, MembershipRole.OWNER);
        SubscriptionMembership member = membership(ALICE, shared, MembershipRole.MEMBER);
        SubscriptionMembership third = membership(CAROL, shared, MembershipRole.MEMBER);
        stubMemberships(List.of(member), List.of(owner, member, third));

        DashboardSummaryResponse result = dashboardService.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("3.33");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("39.96");
    }

    @Test
    void getSummary_shouldRejectInconsistentMembershipCount() {
        SubscriptionMembership owner =
                membership(ALICE, subscription(13L, "Invalid").build(), MembershipRole.OWNER);
        stubMemberships(List.of(owner), List.of());

        assertThatThrownBy(() -> dashboardService.getSummary(ALICE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getUpcomingPayments_shouldIncludeTodayAndDayThirtyButNotPastOrDayThirtyOne() {
        List<SubscriptionMembership> mine = List.of(
                membership(
                        ALICE,
                        subscription(10L, "Thirty")
                                .withNextPaymentDate(TODAY.plusDays(30))
                                .build(),
                        MembershipRole.OWNER),
                membership(
                        ALICE,
                        subscription(11L, "Later")
                                .withNextPaymentDate(TODAY.plusDays(31))
                                .build(),
                        MembershipRole.OWNER),
                membership(
                        ALICE,
                        subscription(8L, "Past")
                                .withNextPaymentDate(TODAY.minusDays(1))
                                .build(),
                        MembershipRole.OWNER),
                membership(
                        ALICE,
                        subscription(9L, "Today").withNextPaymentDate(TODAY).build(),
                        MembershipRole.OWNER));
        stubMemberships(mine, mine);
        stubUserIn(DEFAULT_ZONE);

        List<UpcomingPaymentResponse> result = dashboardService.getUpcomingPayments(ALICE);

        assertThat(result)
                .extracting(UpcomingPaymentResponse::providerName, UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(tuple("Today", TODAY), tuple("Thirty", TODAY.plusDays(30)));
    }

    @Test
    void getUpcomingPayments_shouldKeepOriginalCurrencyAndUsersShare() {
        Subscription shared = subscription(12L, "British TV")
                .withPrice("10.00")
                .withCurrency(Currency.GBP)
                .build();
        SubscriptionMembership owner = membership(BOB, shared, MembershipRole.OWNER);
        SubscriptionMembership member = membership(ALICE, shared, MembershipRole.MEMBER);
        stubMemberships(List.of(member), List.of(owner, member));
        stubUserIn(DEFAULT_ZONE);

        List<UpcomingPaymentResponse> result = dashboardService.getUpcomingPayments(ALICE);

        assertThat(result).singleElement().satisfies(payment -> {
            assertThat(payment.subscriptionId()).isEqualTo(12L);
            assertThat(payment.amount()).isEqualByComparingTo("5.00");
            assertThat(payment.currency()).isEqualTo(Currency.GBP);
        });
    }

    @Test
    void getUpcomingPayments_shouldStartFromUsersToday() {
        SubscriptionMembership dueToday = membership(
                ALICE, subscription(1L, "Due today").withNextPaymentDate(TODAY).build(), MembershipRole.OWNER);
        SubscriptionMembership dueTokyoToday = membership(
                ALICE,
                subscription(2L, "Due Tokyo today")
                        .withNextPaymentDate(TOKYO_TODAY)
                        .build(),
                MembershipRole.OWNER);
        stubMemberships(List.of(dueToday, dueTokyoToday), List.of(dueToday, dueTokyoToday));
        stubUserIn(TOKYO);

        assertThat(dashboardService.getUpcomingPayments(ALICE))
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TOKYO_TODAY);
    }

    private void stubUserIn(ZoneId zone) {
        User user = TestUsers.user(ALICE);
        user.setTimeZone(zone);
        when(userRepository.getByUsername(ALICE)).thenReturn(user);
    }

    private void stubMyMemberships(List<SubscriptionMembership> mine) {
        when(membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        ALICE, SubscriptionStatus.ACTIVE))
                .thenReturn(mine);
    }

    private void stubMemberships(List<SubscriptionMembership> mine, List<SubscriptionMembership> all) {
        stubMyMemberships(mine);
        when(membershipRepository.findBySubscriptionIdIn(anyCollection())).thenReturn(all);
    }

    private static TestSubscriptions.Builder subscription(long id, String providerName) {
        return aSubscription()
                .withId(id)
                .withProvider(SubscriptionProvider.catalog(providerName, TestProviders.category()));
    }

    private static SubscriptionMembership membership(String username, Subscription subscription, MembershipRole role) {
        return new SubscriptionMembership(TestUsers.user(username), subscription, role);
    }
}
