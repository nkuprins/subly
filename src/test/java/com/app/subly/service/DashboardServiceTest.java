package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestClock.TOKYO_TODAY;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.model.category.Category;
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

    User alice;

    DashboardService service;

    @BeforeEach
    void setUp() {
        alice = TestUsers.user(ALICE);
        when(userRepository.getByUsername(ALICE)).thenReturn(alice);
        stubActiveShares(List.of());
        stubCancelledShares(List.of());
        service = new DashboardService(subscriptionShares, userRepository, TestClock.CLOCK);
    }

    @Test
    void getDashboard_shouldReturnEmptyDashboardWhenUserHasNoSubscriptions() {
        DashboardResponse result = service.getDashboard(ALICE, Currency.EUR);

        DashboardSummaryResponse zero = summary("0.00", "0.00", Currency.EUR, 0);
        assertThat(result.summary()).isEqualTo(zero);
        assertThat(result.individualSummary()).isEqualTo(zero);
        assertThat(result.sharedSummary()).isEqualTo(zero);
        assertThat(result.upcoming()).isEmpty();
        assertThat(result.categories()).isEmpty();
        assertThat(result.savings()).isEqualTo(money("0.00", Currency.EUR));
    }

    @Test
    void getDashboard_shouldConvertCurrenciesIntoMonthlyAndYearlyTotals() {
        stubActiveShares(List.of(
                share(aSubscription().withPrice("12.00")),
                share(aSubscription().withPrice("10.00").withCurrency(Currency.USD)),
                share(aSubscription()
                        .withPrice("100.00")
                        .withBillingCycle(BillingCycle.YEARLY)
                        .withCurrency(Currency.GBP))));

        assertThat(service.getDashboard(ALICE, Currency.EUR).summary())
                .isEqualTo(summary("30.90", "370.80", Currency.EUR, 3));
    }

    @Test
    void getDashboard_shouldIncludeFreeTrialAtItsFuturePrice() {
        stubActiveShares(List.of(share(aSubscription().withPrice("20.00").withFreeTrial())));

        assertThat(service.getDashboard(ALICE, Currency.EUR).summary())
                .isEqualTo(summary("20.00", "240.00", Currency.EUR, 1));
    }

    @Test
    void getDashboard_shouldRoundOnlyTheFinalMonthlyAverageOfYearlySubscription() {
        stubActiveShares(List.of(share(aSubscription().withPrice("100.00").withBillingCycle(BillingCycle.YEARLY))));

        assertThat(service.getDashboard(ALICE, Currency.EUR).summary())
                .isEqualTo(summary("8.33", "100.00", Currency.EUR, 1));
    }

    @Test
    void getDashboard_shouldUseTheUsersShareInsteadOfThePrice() {
        stubActiveShares(List.of(share(aSubscription().withPrice("10.00"), "3.34")));

        assertThat(service.getDashboard(ALICE, Currency.EUR).summary())
                .isEqualTo(summary("3.34", "40.08", Currency.EUR, 1));
    }

    @Test
    void getDashboard_shouldConvertTotalsIntoTargetCurrency() {
        stubActiveShares(List.of(share(aSubscription().withPrice("8.90"))));

        assertThat(service.getDashboard(ALICE, Currency.USD).summary())
                .isEqualTo(summary("10.00", "120.00", Currency.USD, 1));
    }

    @Test
    void getDashboard_shouldSplitSummaryIntoIndividualAndSharedSubscriptions() {
        stubActiveShares(List.of(
                share(aSubscription().withPrice("12.00")),
                share(aSubscription().withPrice("10.00").withMaxParticipants(2), "5.00")));

        DashboardResponse result = service.getDashboard(ALICE, Currency.EUR);

        assertThat(result.individualSummary()).isEqualTo(summary("12.00", "144.00", Currency.EUR, 1));
        assertThat(result.sharedSummary()).isEqualTo(summary("5.00", "60.00", Currency.EUR, 1));
        assertThat(result.summary()).isEqualTo(summary("17.00", "204.00", Currency.EUR, 2));
    }

    @Test
    void getDashboard_shouldListUpcomingPaymentsFromTodayToDayThirty() {
        stubActiveShares(List.of(
                share(aSubscription().withNextPaymentDate(TODAY.minusDays(1))),
                share(aSubscription().withNextPaymentDate(TODAY)),
                share(aSubscription().withNextPaymentDate(TODAY.plusDays(30))),
                share(aSubscription().withNextPaymentDate(TODAY.plusDays(31)))));

        assertThat(service.getDashboard(ALICE, Currency.EUR).upcoming())
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TODAY, TODAY.plusDays(30));
    }

    @Test
    void getDashboard_shouldConvertUpcomingPaymentIntoTargetCurrency() {
        stubActiveShares(List.of(share(
                aSubscription()
                        .withId(1L)
                        .withPrice("10.00")
                        .withCurrency(Currency.GBP)
                        .withNextPaymentDate(TODAY.plusDays(2)),
                "5.00")));

        assertThat(service.getDashboard(ALICE, Currency.USD).upcoming())
                .containsExactly(new UpcomingPaymentResponse(
                        1L,
                        TestProviders.CATALOG_PROVIDER_NAME,
                        new BigDecimal("6.74"),
                        Currency.USD,
                        TODAY.plusDays(2),
                        2));
    }

    @Test
    void getDashboard_shouldStartUpcomingPaymentsFromUsersToday() {
        alice.setTimeZone(TOKYO);
        stubActiveShares(List.of(
                share(aSubscription().withNextPaymentDate(TODAY)),
                share(aSubscription().withNextPaymentDate(TOKYO_TODAY))));

        assertThat(service.getDashboard(ALICE, Currency.EUR).upcoming())
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TOKYO_TODAY);
    }

    @Test
    void getDashboard_shouldSumSpendingPerCategoryAndSortByYearlyTotal() {
        Category music = category(1L, "Music");
        Category streaming = category(2L, "Streaming");
        stubActiveShares(List.of(
                share(subscriptionIn(music).withPrice("10.00")),
                share(subscriptionIn(music).withPrice("10.00").withCurrency(Currency.USD)),
                share(subscriptionIn(streaming).withPrice("300.00").withBillingCycle(BillingCycle.YEARLY))));

        assertThat(service.getDashboard(ALICE, Currency.EUR).categories())
                .containsExactly(
                        categorySpending(2L, "Streaming", "25.00", "300.00", Currency.EUR),
                        categorySpending(1L, "Music", "18.90", "226.80", Currency.EUR));
    }

    @Test
    void getDashboard_shouldSumSavingsOfCancelledSubscriptionsFromTheEndDate() {
        stubCancelledShares(List.of(
                cancelledShare("10.00", Currency.EUR, TODAY.minusDays(1)),
                cancelledShare("5.00", Currency.EUR, TODAY.minusMonths(1)),
                cancelledShare("7.00", Currency.USD, TODAY.minusDays(1))));

        assertThat(service.getDashboard(ALICE, Currency.EUR).savings()).isEqualTo(money("26.23", Currency.EUR));
    }

    @Test
    void getDashboard_shouldHaveNoSavingsBeforeTheEndDate() {
        stubCancelledShares(List.of(cancelledShare("10.00", Currency.EUR, TODAY.plusDays(3))));

        assertThat(service.getDashboard(ALICE, Currency.EUR).savings()).isEqualTo(money("0.00", Currency.EUR));
    }

    private void stubActiveShares(List<Share> shares) {
        when(subscriptionShares.load(ALICE, SubscriptionStatus.ACTIVE)).thenReturn(shares);
    }

    private void stubCancelledShares(List<Share> shares) {
        when(subscriptionShares.load(ALICE, SubscriptionStatus.CANCELLED)).thenReturn(shares);
    }

    private static TestSubscriptions.Builder subscriptionIn(Category category) {
        return aSubscription().withProvider(TestProviders.catalog(category));
    }

    private static Category category(long id, String name) {
        Category category = new Category(name);
        category.setId(id);
        return category;
    }

    private static Share share(TestSubscriptions.Builder subscription) {
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

    private static DashboardSummaryResponse summary(String monthly, String yearly, Currency currency, int count) {
        return new DashboardSummaryResponse(new BigDecimal(monthly), new BigDecimal(yearly), currency, count);
    }

    private static CategorySpendingResponse categorySpending(
            long id, String name, String monthly, String yearly, Currency currency) {
        return new CategorySpendingResponse(id, name, new BigDecimal(monthly), new BigDecimal(yearly), currency);
    }

    private static MoneyResponse money(String amount, Currency currency) {
        return new MoneyResponse(new BigDecimal(amount), currency);
    }
}
