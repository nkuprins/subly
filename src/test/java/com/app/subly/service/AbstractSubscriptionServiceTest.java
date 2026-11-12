package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.DEFAULT_NEXT_PAYMENT_DATE;
import static com.app.subly.testdata.TestSubscriptions.DEFAULT_PRICE;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.CreateSubscriptionRequest;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.user.User;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.enums.ProviderType;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.service.access.SubscriptionAccess;
import com.app.subly.service.share.MemberShareLoader;
import com.app.subly.testdata.TestClock;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class AbstractSubscriptionServiceTest {

    static final long SUBSCRIPTION_ID = 7L;
    static final long PROVIDER_ID = 1L;
    static final long ALICE_ID = 1L;
    static final long BOB_ID = 2L;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @Mock
    SubscriptionProviderRepository subscriptionProviderRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    SubscriptionAccess subscriptionAccess;

    @Mock
    MemberShareLoader memberShareLoader;

    SubscriptionService service;
    final User alice = TestUsers.userWithId(ALICE, ALICE_ID);
    final User bob = TestUsers.userWithId(BOB, BOB_ID);

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(
                subscriptionRepository,
                subscriptionAccess,
                membershipRepository,
                subscriptionProviderRepository,
                userRepository,
                memberShareLoader,
                TestClock.CLOCK);
    }

    void stubAliceExists() {
        when(userRepository.getUser(ALICE)).thenReturn(alice);
    }

    void stubOwnedSubscription(Subscription subscription) {
        when(subscriptionAccess.getOwnedSubscription(ALICE, SUBSCRIPTION_ID)).thenReturn(subscription);
    }

    void stubOwnerMembership(Subscription subscription) {
        when(subscriptionAccess.getOwnerMembership(ALICE, SUBSCRIPTION_ID))
                .thenReturn(SubscriptionMembership.owner(alice, subscription));
    }

    static void assertPaymentDetails(
            Subscription subscription,
            BigDecimal price,
            BillingCycle billingCycle,
            LocalDate nextPaymentDate,
            boolean freeTrial) {
        assertThat(subscription.getPrice()).isEqualByComparingTo(price);
        assertThat(subscription.getBillingCycle()).isEqualTo(billingCycle);
        assertThat(subscription.getNextPaymentDate()).isEqualTo(nextPaymentDate);
        assertThat(subscription.getBillingAnchorDate()).isEqualTo(nextPaymentDate);
        assertThat(subscription.isFreeTrial()).isEqualTo(freeTrial);
    }

    SubscriptionProvider provider(ProviderType providerType) {
        return providerType == ProviderType.CATALOG ? catalogProvider() : customProvider();
    }

    SubscriptionProvider catalogProvider() {
        return TestProviders.catalog(TestProviders.category());
    }

    SubscriptionProvider customProvider() {
        return TestProviders.custom(TestProviders.category(), alice);
    }

    Subscription subscription(SubscriptionProvider provider) {
        return aSubscription().withProvider(provider).build();
    }

    static CreateSubscriptionRequest createRequest() {
        return createRequest(DEFAULT_NEXT_PAYMENT_DATE);
    }

    static CreateSubscriptionRequest createRequest(LocalDate nextPaymentDate) {
        return new CreateSubscriptionRequest(
                PROVIDER_ID, DEFAULT_PRICE, Currency.USD, BillingCycle.MONTHLY, nextPaymentDate, false, 1);
    }
}
