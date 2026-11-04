package com.app.subly.repository;

import com.app.subly.models.User;
import com.app.subly.models.UserRole;
import com.app.subly.models.subscription.BillingCycle;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.ServiceProvider;
import com.app.subly.models.subscription.Subscription;
import com.app.subly.models.subscription.SubscriptionMembership;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionMembershipRepositoryTest {

    static final String OWNER = "test-owner";
    static final String MEMBER = "test-member";
    static final String STRANGER = "test-stranger";

    @Autowired SubscriptionMembershipRepository membershipRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired TestEntityManager entityManager;

    Subscription subscription;
    SubscriptionMembership ownerRow;
    SubscriptionMembership memberRow;

    @BeforeEach
    void setUp() {
        User owner = persistUser(OWNER);
        User member = persistUser(MEMBER);
        persistUser(STRANGER);
        Category gym = entityManager.persist(new Category("Test gym"));
        ServiceProvider service = entityManager.persist(ServiceProvider.custom("My gym", gym, owner));
        LocalDate today = LocalDate.now();
        subscription = entityManager.persist(new Subscription(service, today, new BigDecimal("35.00"),
                BillingCycle.MONTHLY, today.plusDays(10), false));
        ownerRow = entityManager.persist(new SubscriptionMembership(owner, subscription, MembershipRole.OWNER));
        memberRow = entityManager.persist(new SubscriptionMembership(member, subscription, MembershipRole.MEMBER));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void existsByOwnerRoleFindsTheOwner() {
        assertThat(membershipRepository.existsBySubscriptionIdAndUserUsernameAndRole(
                subscription.getId(), OWNER, MembershipRole.OWNER)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {MEMBER, STRANGER})
    void existsByOwnerRoleIgnoresNonOwners(String username) {
        assertThat(membershipRepository.existsBySubscriptionIdAndUserUsernameAndRole(
                subscription.getId(), username, MembershipRole.OWNER)).isFalse();
    }

    @Test
    void deletingSubscriptionDeletesItsMemberships() {
        subscriptionRepository.deleteById(subscription.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(membershipRepository.existsById(ownerRow.getId())).isFalse();
        assertThat(membershipRepository.existsById(memberRow.getId())).isFalse();
    }

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setName(username);
        user.setEmail(username + "@test.local");
        user.setPasswordHash("not-a-real-hash");
        user.setRole(UserRole.USER);
        return entityManager.persist(user);
    }
}
