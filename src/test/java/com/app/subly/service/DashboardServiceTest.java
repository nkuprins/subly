package com.app.subly.service;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestClock.TOKYO_TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Currency;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.model.user.User;
import com.app.subly.repository.UserRepository;
import com.app.subly.service.SubscriptionShares.Share;
import com.app.subly.testdata.TestClock;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestSubscriptions;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
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
    SubscriptionShares subscriptionShares;

    @Mock
    UserRepository userRepository;

    DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(subscriptionShares, userRepository, TestClock.CLOCK);
    }

    @Test
    void getSummary_shouldReturnZeroWhenUserHasNoSubscriptions() {
        stubActiveShares(List.of());

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("0.00");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("0.00");
        assertThat(result.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void getSummary_shouldConvertCurrenciesIntoMonthlyAndYearlyTotals() {
        stubActiveShares(List.of(
                fullShare(subscription(1L, "Netflix").withPrice("12.00")),
                fullShare(subscription(2L, "YouTube").withPrice("10.00").withCurrency(Currency.USD)),
                fullShare(subscription(3L, "Gym")
                        .withPrice("100.00")
                        .withBillingCycle(BillingCycle.YEARLY)
                        .withCurrency(Currency.GBP))));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("30.90");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("370.80");
        assertThat(result.currency()).isEqualTo(Currency.EUR);
    }

    @Test
    void getSummary_shouldIncludeFreeTrialAtItsFuturePrice() {
        stubActiveShares(
                List.of(fullShare(subscription(4L, "Trial").withPrice("20.00").withFreeTrial())));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("20.00");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("240.00");
    }

    @Test
    void getSummary_shouldRoundOnlyTheFinalMonthlyAverageOfYearlySubscription() {
        stubActiveShares(List.of(
                fullShare(subscription(5L, "Yearly").withPrice("100.00").withBillingCycle(BillingCycle.YEARLY))));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("8.33");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void getSummary_shouldUseTheUsersShareInsteadOfThePrice() {
        stubActiveShares(List.of(share(subscription(6L, "Shared").withPrice("10.00"), "3.34")));

        DashboardSummaryResponse result = service.getSummary(ALICE);

        assertThat(result.monthlyTotal()).isEqualByComparingTo("3.34");
        assertThat(result.yearlyTotal()).isEqualByComparingTo("40.08");
    }

    @Test
    void getUpcomingPayments_shouldIncludeTodayAndDayThirtyButNotPastOrDayThirtyOne() {
        stubActiveShares(List.of(
                fullShare(subscription(10L, "Thirty").withNextPaymentDate(TODAY.plusDays(30))),
                fullShare(subscription(11L, "Later").withNextPaymentDate(TODAY.plusDays(31))),
                fullShare(subscription(8L, "Past").withNextPaymentDate(TODAY.minusDays(1))),
                fullShare(subscription(9L, "Today").withNextPaymentDate(TODAY))));
        stubUserIn(DEFAULT_ZONE);

        List<UpcomingPaymentResponse> result = service.getUpcomingPayments(ALICE);

        assertThat(result)
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TODAY, TODAY.plusDays(30));
    }

    @Test
    void getUpcomingPayments_shouldKeepOriginalCurrencyAndUsersShare() {
        long daysLeft = 2;
        Share share = share(
                subscription(12L, "British TV")
                        .withPrice("10.00")
                        .withCurrency(Currency.GBP)
                        .withNextPaymentDate(TODAY.plusDays(daysLeft)),
                "5.00");
        Subscription shared = share.subscription();
        stubActiveShares(List.of(share));
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
        stubActiveShares(List.of(
                fullShare(subscription(1L, "Due today").withNextPaymentDate(TODAY)),
                fullShare(subscription(2L, "Due Tokyo today").withNextPaymentDate(TOKYO_TODAY))));
        stubUserIn(TOKYO);

        assertThat(service.getUpcomingPayments(ALICE))
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TOKYO_TODAY);
    }

    @Test
    void getSavings_shouldSumCancelledSubscriptionsFromTheEndDateInEur() {
        stubCancelledShares(List.of(
                cancelledShare("10.00", Currency.EUR, TODAY.minusDays(1)),
                cancelledShare("5.00", Currency.EUR, TODAY.minusMonths(1)),
                cancelledShare("7.00", Currency.USD, TODAY.minusDays(1))));
        stubUserIn(DEFAULT_ZONE);

        assertThat(service.getSavings(ALICE)).isEqualTo(new MoneyResponse(new BigDecimal("26.23"), Currency.EUR));
    }

    @Test
    void getSavings_shouldBeZeroBeforeTheEndDate() {
        stubCancelledShares(List.of(cancelledShare("10.00", Currency.EUR, TODAY.plusDays(3))));
        stubUserIn(DEFAULT_ZONE);

        assertThat(service.getSavings(ALICE)).isEqualTo(new MoneyResponse(new BigDecimal("0.00"), Currency.EUR));
    }

    private void stubCancelledShares(List<Share> shares) {
        when(subscriptionShares.load(ALICE, SubscriptionStatus.CANCELLED)).thenReturn(shares);
    }

    private void stubUserIn(ZoneId zone) {
        User user = TestUsers.user(ALICE);
        user.setTimeZone(zone);
        when(userRepository.getByUsername(ALICE)).thenReturn(user);
    }

    private void stubActiveShares(List<Share> shares) {
        when(subscriptionShares.load(ALICE, SubscriptionStatus.ACTIVE)).thenReturn(shares);
    }

    private static TestSubscriptions.Builder subscription(long id, String providerName) {
        return aSubscription()
                .withId(id)
                .withProvider(SubscriptionProvider.catalog(providerName, TestProviders.category()));
    }

    private static Share fullShare(TestSubscriptions.Builder subscription) {
        Subscription built = subscription.build();
        return new Share(SubscriptionMembership.owner(TestUsers.user(ALICE), built), built.getPrice());
    }

    private static Share share(TestSubscriptions.Builder subscription, String amount) {
        return new Share(
                SubscriptionMembership.owner(TestUsers.user(ALICE), subscription.build()), new BigDecimal(amount));
    }

    private static Share cancelledShare(String amount, Currency currency, LocalDate endDate) {
        return share(
                aSubscription()
                        .withCurrency(currency)
                        .withNextPaymentDate(endDate)
                        .cancelled(),
                amount);
    }
}
