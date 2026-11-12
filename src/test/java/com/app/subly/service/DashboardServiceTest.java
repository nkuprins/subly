package com.app.subly.service;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestClock.TOKYO_TODAY;
import static com.app.subly.testdata.TestMemberShares.cancelledShare;
import static com.app.subly.testdata.TestMemberShares.share;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.MoneyResponse;
import com.app.subly.dto.dashboard.CategorySpendingResponse;
import com.app.subly.dto.dashboard.DashboardResponse;
import com.app.subly.dto.dashboard.DashboardSummaryResponse;
import com.app.subly.dto.dashboard.UpcomingPaymentResponse;
import com.app.subly.entity.user.User;
import com.app.subly.enums.Currency;
import com.app.subly.enums.SubscriptionStatus;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestClock;
import com.app.subly.testdata.TestUsers;
import com.app.subly.util.MemberShare;
import java.math.BigDecimal;
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
        service = new DashboardService(subscriptionShares, userRepository, TestClock.CLOCK);
    }

    @Test
    void getDashboard_shouldSplitSummaryIntoIndividualAndSharedSubscriptions() {
        stubActiveShares(List.of(
                share(aSubscription().withPrice("12.00")),
                share(aSubscription().withPrice("10.00").withMaxParticipants(2), "5.00")));
        stubCancelledShares(List.of());

        DashboardResponse result = service.getDashboard(ALICE, Currency.EUR);

        assertThat(result.individualSummary()).isEqualTo(summary("12.00", "144.00", 1));
        assertThat(result.sharedSummary()).isEqualTo(summary("5.00", "60.00", 1));
        assertThat(result.summary()).isEqualTo(summary("17.00", "204.00", 2));
    }

    @Test
    void getDashboard_shouldStartUpcomingPaymentsFromUsersToday() {
        alice.setTimeZone(TOKYO);
        stubActiveShares(List.of(
                share(aSubscription().withNextPaymentDate(TODAY)),
                share(aSubscription().withNextPaymentDate(TOKYO_TODAY))));
        stubCancelledShares(List.of());

        assertThat(service.getDashboard(ALICE, Currency.EUR).upcoming())
                .extracting(UpcomingPaymentResponse::nextPaymentDate)
                .containsExactly(TOKYO_TODAY);
    }

    @Test
    void getDashboard_shouldTakeCategoriesFromActiveAndSavingsFromCancelledSubscriptions() {
        stubActiveShares(List.of(share(aSubscription().withPrice("10.00"))));
        stubCancelledShares(List.of(cancelledShare("5.00", Currency.EUR, TODAY.minusDays(1))));

        DashboardResponse result = service.getDashboard(ALICE, Currency.EUR);

        assertThat(result.categories())
                .extracting(CategorySpendingResponse::yearlyTotal)
                .containsExactly(new BigDecimal("120.00"));
        assertThat(result.savings()).isEqualTo(new MoneyResponse(new BigDecimal("5.00"), Currency.EUR));
    }

    private void stubActiveShares(List<MemberShare> shares) {
        when(subscriptionShares.forUser(ALICE, SubscriptionStatus.ACTIVE)).thenReturn(shares);
    }

    private void stubCancelledShares(List<MemberShare> shares) {
        when(subscriptionShares.forUser(ALICE, SubscriptionStatus.CANCELLED)).thenReturn(shares);
    }

    private static DashboardSummaryResponse summary(String monthly, String yearly, int count) {
        return new DashboardSummaryResponse(new BigDecimal(monthly), new BigDecimal(yearly), Currency.EUR, count);
    }
}
