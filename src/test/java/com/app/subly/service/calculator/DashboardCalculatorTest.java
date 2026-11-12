package com.app.subly.service.calculator;

import static com.app.subly.service.calculator.DashboardCalculator.savings;
import static com.app.subly.service.calculator.DashboardCalculator.spendingByCategory;
import static com.app.subly.service.calculator.DashboardCalculator.summarize;
import static com.app.subly.service.calculator.DashboardCalculator.upcomingPayments;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestMemberShares.cancelledShare;
import static com.app.subly.testdata.TestMemberShares.share;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.entity.category.Category;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.service.share.MemberShare;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestSubscriptions;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardCalculatorTest {

    @Test
    void summarize_shouldReturnZeroWhenThereAreNoSubscriptions() {
        assertThat(summarize(List.of(), Currency.EUR)).isEqualTo(summary("0.00", "0.00", Currency.EUR, 0));
    }

    @Test
    void summarize_shouldConvertCurrenciesIntoMonthlyAndYearlyTotals() {
        List<MemberShare> shares = List.of(
                share(aSubscription().withPrice("12.00")),
                share(aSubscription().withPrice("10.00").withCurrency(Currency.USD)),
                share(aSubscription()
                        .withPrice("100.00")
                        .withBillingCycle(BillingCycle.YEARLY)
                        .withCurrency(Currency.GBP)));

        assertThat(summarize(shares, Currency.EUR)).isEqualTo(summary("30.90", "370.80", Currency.EUR, 3));
    }

    @Test
    void summarize_shouldRoundOnlyTheFinalMonthlyAverageOfYearlySubscription() {
        List<MemberShare> shares =
                List.of(share(aSubscription().withPrice("100.00").withBillingCycle(BillingCycle.YEARLY)));

        assertThat(summarize(shares, Currency.EUR)).isEqualTo(summary("8.33", "100.00", Currency.EUR, 1));
    }

    @Test
    void summarize_shouldUseTheUsersShareInsteadOfThePrice() {
        List<MemberShare> shares = List.of(share(aSubscription().withPrice("10.00"), "3.34"));

        assertThat(summarize(shares, Currency.EUR)).isEqualTo(summary("3.34", "40.08", Currency.EUR, 1));
    }

    @Test
    void summarize_shouldConvertTotalsIntoTargetCurrency() {
        List<MemberShare> shares = List.of(share(aSubscription().withPrice("8.90")));

        assertThat(summarize(shares, Currency.USD)).isEqualTo(summary("10.00", "120.00", Currency.USD, 1));
    }

    @Test
    void upcomingPayments_shouldListPaymentsFromTodayToDayThirty() {
        List<MemberShare> shares = List.of(
                share(aSubscription().withNextPaymentDate(TODAY.minusDays(1))),
                share(aSubscription().withNextPaymentDate(TODAY)),
                share(aSubscription().withNextPaymentDate(TODAY.plusDays(30))),
                share(aSubscription().withNextPaymentDate(TODAY.plusDays(31))));

        assertThat(upcomingPayments(shares, TODAY, Currency.EUR))
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TODAY, TODAY.plusDays(30));
    }

    @Test
    void upcomingPayments_shouldConvertAmountIntoTargetCurrency() {
        List<MemberShare> shares = List.of(share(
                aSubscription()
                        .withId(1L)
                        .withPrice("10.00")
                        .withCurrency(Currency.GBP)
                        .withNextPaymentDate(TODAY.plusDays(2)),
                "5.00"));

        assertThat(upcomingPayments(shares, TODAY, Currency.USD))
                .containsExactly(new UpcomingPaymentResponse(
                        TestProviders.CATALOG_PROVIDER_NAME,
                        new BigDecimal("6.74"),
                        Currency.USD,
                        TODAY.plusDays(2),
                        2));
    }

    @Test
    void spendingByCategory_shouldSumPerCategoryAndSortByYearlyTotal() {
        Category music = category(1L, "Music");
        Category streaming = category(2L, "Streaming");
        List<MemberShare> shares = List.of(
                share(subscriptionIn(music).withPrice("10.00")),
                share(subscriptionIn(music).withPrice("10.00").withCurrency(Currency.USD)),
                share(subscriptionIn(streaming).withPrice("300.00").withBillingCycle(BillingCycle.YEARLY)));

        assertThat(spendingByCategory(shares, Currency.EUR))
                .containsExactly(
                        categorySpending("Streaming", "25.00", "300.00", Currency.EUR),
                        categorySpending("Music", "18.90", "226.80", Currency.EUR));
    }

    @Test
    void savings_shouldSumCancelledSubscriptionsFromTheEndDate() {
        List<MemberShare> shares = List.of(
                cancelledShare("10.00", Currency.EUR, TODAY.minusDays(1)),
                cancelledShare("5.00", Currency.EUR, TODAY.minusMonths(1)),
                cancelledShare("7.00", Currency.USD, TODAY.minusDays(1)));

        assertThat(savings(shares, TODAY, Currency.EUR)).isEqualTo(money("26.23", Currency.EUR));
    }

    @Test
    void savings_shouldBeZeroBeforeTheEndDate() {
        List<MemberShare> shares = List.of(cancelledShare("10.00", Currency.EUR, TODAY.plusDays(3)));

        assertThat(savings(shares, TODAY, Currency.EUR)).isEqualTo(money("0.00", Currency.EUR));
    }

    private static TestSubscriptions.Builder subscriptionIn(Category category) {
        return aSubscription().withProvider(TestProviders.catalog(category));
    }

    private static Category category(long id, String name) {
        Category category = new Category(name);
        category.setId(id);
        return category;
    }

    private static DashboardSummaryResponse summary(String monthly, String yearly, Currency currency, int count) {
        return new DashboardSummaryResponse(new BigDecimal(monthly), new BigDecimal(yearly), currency, count);
    }

    private static CategorySpendingResponse categorySpending(
            String name, String monthly, String yearly, Currency currency) {
        return new CategorySpendingResponse(name, new BigDecimal(monthly), new BigDecimal(yearly), currency);
    }

    private static MoneyResponse money(String amount, Currency currency) {
        return new MoneyResponse(new BigDecimal(amount), currency);
    }
}
