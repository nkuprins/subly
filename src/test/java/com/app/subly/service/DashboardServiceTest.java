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

    DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(membershipRepository, userRepository, TestClock.CLOCK);
    }

    @Test
    void getSummary_shouldReturnZeroWhenUserHasNoSubscriptions() {
        stubAliceMemberships(List.of());

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("0.00");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("0.00");
        assertThat(result.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void getSummary_shouldConvertCurrenciesIntoMonthlyAndYearlyTotals() {
        SubscriptionMembership netflix = aliceOwns(subscription(1L, "Netflix").withPrice("12.00"));
        SubscriptionMembership youtube =
                aliceOwns(subscription(2L, "YouTube").withPrice("10.00").withCurrency(Currency.USD));
        SubscriptionMembership gym = aliceOwns(subscription(3L, "Gym")
                .withPrice("100.00")
                .withBillingCycle(BillingCycle.YEARLY)
                .withCurrency(Currency.GBP));
        stubAliceAsOnlyMember(List.of(netflix, youtube, gym));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("30.90");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("370.80");
        assertThat(result.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void getSummary_shouldIncludeFreeTrialAtItsFuturePrice() {
        SubscriptionMembership trial =
                aliceOwns(subscription(4L, "Trial").withPrice("20.00").withFreeTrial());
        stubAliceAsOnlyMember(List.of(trial));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("20.00");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("240.00");
    }

    @Test
    void getSummary_shouldRoundOnlyTheFinalMonthlyAverageOfYearlySubscription() {
        SubscriptionMembership yearly =
                aliceOwns(subscription(5L, "Yearly").withPrice("100.00").withBillingCycle(BillingCycle.YEARLY));
        stubAliceAsOnlyMember(List.of(yearly));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("8.33");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void getSummary_shouldGiveOwnerTheRemainderCentOfSharedPrice() {
        Subscription shared = subscription(6L, "Shared").withPrice("10.00").build();
        SubscriptionMembership owner = SubscriptionMembership.owner(TestUsers.user(ALICE), shared);
        SubscriptionMembership bob = member(BOB, shared);
        SubscriptionMembership carol = member(CAROL, shared);
        stubMemberships(List.of(owner), List.of(owner, bob, carol));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("3.34");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("40.08");
    }

    @Test
    void getSummary_shouldNotGiveRemainderCentToRegularMember() {
        Subscription shared = subscription(7L, "Shared").withPrice("10.00").build();
        SubscriptionMembership owner = SubscriptionMembership.owner(TestUsers.user(BOB), shared);
        SubscriptionMembership member = member(ALICE, shared);
        SubscriptionMembership third = member(CAROL, shared);
        stubMemberships(List.of(member), List.of(owner, member, third));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("3.33");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("39.96");
    }

    @Test
    void getSummary_shouldRejectInconsistentMembershipCount() {
        SubscriptionMembership owner = aliceOwns(subscription(13L, "Invalid"));
        stubMemberships(List.of(owner), List.of());

        assertThatThrownBy(() -> service.getSummary(ALICE)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void getUpcomingPayments_shouldIncludeTodayAndDayThirtyButNotPastOrDayThirtyOne() {
        stubAliceAsOnlyMember(List.of(
                aliceOwns(subscription(10L, "Thirty").withNextPaymentDate(TODAY.plusDays(30))),
                aliceOwns(subscription(11L, "Later").withNextPaymentDate(TODAY.plusDays(31))),
                aliceOwns(subscription(8L, "Past").withNextPaymentDate(TODAY.minusDays(1))),
                aliceOwns(subscription(9L, "Today").withNextPaymentDate(TODAY))));
        stubUserIn(DEFAULT_ZONE);

        List<UpcomingPaymentResponse> result = service.getUpcomingPayments(ALICE);

        assertThat(result)
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TODAY, TODAY.plusDays(30));
    }

    @Test
    void getUpcomingPayments_shouldKeepOriginalCurrencyAndUsersShare() {
        long daysLeft = 2;
        Subscription shared = subscription(12L, "British TV")
                .withPrice("10.00")
                .withCurrency(Currency.GBP)
                .withNextPaymentDate(TODAY.plusDays(daysLeft))
                .build();
        SubscriptionMembership owner = SubscriptionMembership.owner(TestUsers.user(BOB), shared);
        SubscriptionMembership member = member(ALICE, shared);
        stubMemberships(List.of(member), List.of(owner, member));
        stubUserIn(DEFAULT_ZONE);

        List<UpcomingPaymentResponse> result = service.getUpcomingPayments(ALICE);

        assertThat(result).singleElement().satisfies(payment -> {
            assertThat(payment.subscriptionId()).isEqualTo(shared.getId());
            assertThat(payment.providerName()).isEqualTo(shared.getProvider().getName());
            assertThat(payment.amount()).isEqualByComparingTo("5.00");
            assertThat(payment.currency()).isEqualTo(Currency.GBP);
            assertThat(payment.nextPaymentDate()).isEqualTo(shared.getNextPaymentDate());
            assertThat(payment.daysLeft()).isEqualTo(daysLeft);
        });
    }

    @Test
    void getUpcomingPayments_shouldStartFromUsersToday() {
        stubAliceAsOnlyMember(List.of(
                aliceOwns(subscription(1L, "Due today").withNextPaymentDate(TODAY)),
                aliceOwns(subscription(2L, "Due Tokyo today").withNextPaymentDate(TOKYO_TODAY))));
        stubUserIn(TOKYO);

        assertThat(service.getUpcomingPayments(ALICE))
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TOKYO_TODAY);
    }

    private void stubUserIn(ZoneId zone) {
        User user = TestUsers.user(ALICE);
        user.setTimeZone(zone);
        when(userRepository.getByUsername(ALICE)).thenReturn(user);
    }

    private void stubAliceMemberships(List<SubscriptionMembership> aliceMemberships) {
        when(membershipRepository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        ALICE, SubscriptionStatus.ACTIVE))
                .thenReturn(aliceMemberships);
    }

    private void stubAliceAsOnlyMember(List<SubscriptionMembership> aliceMemberships) {
        stubMemberships(aliceMemberships, aliceMemberships);
    }

    private void stubMemberships(
            List<SubscriptionMembership> aliceMemberships, List<SubscriptionMembership> allMemberships) {
        stubAliceMemberships(aliceMemberships);
        List<Long> subscriptionIds = aliceMemberships.stream()
                .map(membership -> membership.getSubscription().getId())
                .toList();
        when(membershipRepository.findBySubscriptionIdIn(subscriptionIds)).thenReturn(allMemberships);
    }

    private static TestSubscriptions.Builder subscription(long id, String providerName) {
        return aSubscription()
                .withId(id)
                .withProvider(SubscriptionProvider.catalog(providerName, TestProviders.category()));
    }

    private static SubscriptionMembership aliceOwns(TestSubscriptions.Builder subscription) {
        return SubscriptionMembership.owner(TestUsers.user(ALICE), subscription.build());
    }

    private static SubscriptionMembership member(String username, Subscription subscription) {
        return new SubscriptionMembership(TestUsers.user(username), subscription, MembershipRole.MEMBER);
    }
}
