package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.app.subly.dto.PaymentDetails;
import com.app.subly.model.User;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.ServiceProvider;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.ServiceProviderRepository;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class AbstractSubscriptionServiceTest {

    static final long SUBSCRIPTION_ID = 7L;
    static final long SERVICE_ID = 1L;
    static final long GYM_ID = 4L;
    static final String CUSTOM_NAME = "My gym";
    static final String CATALOG_NAME = "Test catalog";
    static final String OWNER = "test-owner";
    static final String STRANGER = "test-stranger";
    static final BigDecimal ORIGINAL_PRICE = new BigDecimal("35.00");

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @Mock
    ServiceProviderRepository serviceProviderRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserRepository userRepository;

    final Clock clock = Clock.fixed(Instant.parse("2026-11-04T16:00:00Z"), ZoneOffset.UTC);
    SubscriptionService subscriptionService;

    LocalDate today;
    User owner;
    Category gym;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                new SubscriptionAccess(membershipRepository),
                membershipRepository,
                serviceProviderRepository,
                categoryRepository,
                userRepository,
                clock);
        today = LocalDate.of(2026, 11, 4);
        owner = new User();
        owner.setUsername(OWNER);
        gym = new Category("Test gym");
    }

    void givenOwnerExists() {
        given(userRepository.findByUsername(OWNER)).willReturn(Optional.of(owner));
    }

    Subscription givenOwnedCustomSubscription() {
        return givenOwnedSubscription(customService());
    }

    Subscription givenOwnedCatalogSubscription() {
        return givenOwnedSubscription(catalogService());
    }

    Subscription givenSubscriptionOwnedBySomeoneElse() {
        Subscription subscription = subscription(customService());
        given(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .willReturn(Optional.of(SubscriptionMembership.owner(owner, subscription)));
        return subscription;
    }

    static void assertPaymentDetails(Subscription subscription, PaymentDetails request) {
        assertThat(subscription.getPrice()).isEqualByComparingTo(request.price());
        assertThat(subscription.getBillingCycle()).isEqualTo(request.billingCycle());
        assertThat(subscription.getNextPaymentDate()).isEqualTo(request.nextPaymentDate());
        assertThat(subscription.getBillingAnchorDate()).isEqualTo(request.nextPaymentDate());
        assertThat(subscription.isFreeTrial()).isEqualTo(request.freeTrial());
    }

    ServiceProvider catalogService() {
        return ServiceProvider.catalog(CATALOG_NAME, gym);
    }

    ServiceProvider customService() {
        return ServiceProvider.custom(CUSTOM_NAME, gym, owner);
    }

    Subscription subscription(ServiceProvider serviceProvider) {
        return new Subscription(serviceProvider, ORIGINAL_PRICE, BillingCycle.MONTHLY, today.plusDays(10), false);
    }

    private Subscription givenOwnedSubscription(ServiceProvider serviceProvider) {
        Subscription subscription = subscription(serviceProvider);
        given(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .willReturn(Optional.of(SubscriptionMembership.owner(owner, subscription)));
        return subscription;
    }
}
