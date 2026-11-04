package com.app.subly.services;

import com.app.subly.dto.PaymentDetails;
import com.app.subly.models.User;
import com.app.subly.models.subscription.BillingCycle;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.SubscriptionProvider;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
abstract class AbstractSubscriptionServiceTest {

    static final long SUBSCRIPTION_ID = 7L;
    static final long PROVIDER_ID = 1L;
    static final long GYM_ID = 4L;
    static final String CUSTOM_NAME = "My gym";
    static final String CATALOG_NAME = "Test catalog";
    static final String OWNER = "test-owner";
    static final String STRANGER = "test-stranger";
    static final BigDecimal ORIGINAL_PRICE = new BigDecimal("35.00");

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock SubscriptionMembershipRepository membershipRepository;
    @Mock
    SubscriptionProviderRepository providerRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock UserRepository userRepository;

    @InjectMocks SubscriptionService subscriptionService;

    LocalDate today;
    User owner;
    Category gym;

    @BeforeEach
    void setUp() {
        today = LocalDate.now();
        owner = new User();
        owner.setUsername(OWNER);
        gym = new Category("Test gym");
    }

    Subscription givenOwnedCustomSubscription() {
        return givenOwnedSubscription(customProvider());
    }

    Subscription givenOwnedCatalogSubscription() {
        return givenOwnedSubscription(catalogProvider());
    }

    Subscription givenSubscriptionOwnedBySomeoneElse() {
        Subscription subscription = subscription(customProvider());
        given(subscriptionRepository.findWithProviderById(SUBSCRIPTION_ID)).willReturn(Optional.of(subscription));
        given(membershipRepository.existsBySubscriptionIdAndUserUsernameAndRole(
                SUBSCRIPTION_ID, STRANGER, MembershipRole.OWNER)).willReturn(false);
        return subscription;
    }

    static void assertPaymentDetails(Subscription subscription, PaymentDetails request) {
        assertThat(subscription.getPrice()).isEqualByComparingTo(request.price());
        assertThat(subscription.getBillingCycle()).isEqualTo(request.billingCycle());
        assertThat(subscription.getNextPaymentDate()).isEqualTo(request.nextPaymentDate());
        assertThat(subscription.isFreeTrial()).isEqualTo(request.freeTrial());
    }

    SubscriptionProvider catalogProvider() {
        return SubscriptionProvider.catalog(CATALOG_NAME, gym);
    }

    SubscriptionProvider customProvider() {
        return SubscriptionProvider.custom(CUSTOM_NAME, gym, owner);
    }

    Subscription subscription(SubscriptionProvider provider) {
        return new Subscription(provider, today, ORIGINAL_PRICE, BillingCycle.MONTHLY,
                today.plusDays(10), false);
    }

    private Subscription givenOwnedSubscription(SubscriptionProvider provider) {
        Subscription subscription = subscription(provider);
        given(subscriptionRepository.findWithProviderById(SUBSCRIPTION_ID)).willReturn(Optional.of(subscription));
        given(membershipRepository.existsBySubscriptionIdAndUserUsernameAndRole(
                SUBSCRIPTION_ID, OWNER, MembershipRole.OWNER)).willReturn(true);
        return subscription;
    }
}
