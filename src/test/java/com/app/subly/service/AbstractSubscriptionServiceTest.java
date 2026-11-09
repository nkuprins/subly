package com.app.subly.service;

import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.app.subly.dto.subscription.PaymentDetails;
import com.app.subly.model.category.Category;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.user.User;
import com.app.subly.repository.SubscriptionMembershipRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.SubscriptionRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestClock;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
abstract class AbstractSubscriptionServiceTest {

    static final long SUBSCRIPTION_ID = 7L;
    static final long PROVIDER_ID = 1L;

    @Mock
    SubscriptionRepository subscriptionRepository;

    @Mock
    SubscriptionMembershipRepository membershipRepository;

    @Mock
    SubscriptionProviderRepository subscriptionProviderRepository;

    @Mock
    UserRepository userRepository;

    SubscriptionService subscriptionService;
    User owner;
    Category category;

    @BeforeEach
    void setUp() {
        subscriptionService = new SubscriptionService(
                subscriptionRepository,
                new SubscriptionAccess(membershipRepository),
                membershipRepository,
                subscriptionProviderRepository,
                userRepository,
                TestClock.CLOCK);
        owner = TestUsers.user(ALICE);
        category = TestProviders.category();
    }

    void stubOwnerExists() {
        when(userRepository.getByUsername(ALICE)).thenReturn(owner);
    }

    Subscription stubOwnedSubscription(ProviderType providerType) {
        Subscription subscription =
                subscription(providerType == ProviderType.CATALOG ? catalogProvider() : customProvider());
        when(membershipRepository.findBySubscriptionIdAndRole(SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.of(SubscriptionMembership.owner(owner, subscription)));
        return subscription;
    }

    Subscription stubSubscriptionOwnedBySomeoneElse() {
        return stubOwnedSubscription(ProviderType.CUSTOM);
    }

    void stubOwnerRowWithSubscriptionDetails(Subscription subscription) {
        when(membershipRepository.findWithSubscriptionDetailsBySubscriptionIdAndRole(
                        SUBSCRIPTION_ID, MembershipRole.OWNER))
                .thenReturn(Optional.of(SubscriptionMembership.owner(owner, subscription)));
    }

    static void assertPaymentDetails(Subscription subscription, PaymentDetails request) {
        assertThat(subscription.getPrice()).isEqualByComparingTo(request.price());
        assertThat(subscription.getBillingCycle()).isEqualTo(request.billingCycle());
        assertThat(subscription.getNextPaymentDate()).isEqualTo(request.nextPaymentDate());
        assertThat(subscription.getBillingAnchorDate()).isEqualTo(request.nextPaymentDate());
        assertThat(subscription.isFreeTrial()).isEqualTo(request.freeTrial());
    }

    SubscriptionProvider catalogProvider() {
        return TestProviders.catalog(category);
    }

    SubscriptionProvider customProvider() {
        return TestProviders.custom(category, owner);
    }

    Subscription subscription(SubscriptionProvider provider) {
        return aSubscription().withProvider(provider).build();
    }
}
