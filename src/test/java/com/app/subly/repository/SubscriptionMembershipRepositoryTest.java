package com.app.subly.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.model.TestUsers;
import com.app.subly.model.User;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionProvider;
import com.app.subly.model.subscription.SubscriptionStatus;
import jakarta.persistence.PersistenceUnitUtil;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionMembershipRepositoryTest {

    static final String OWNER = "test-owner";
    static final String MEMBER = "test-member";
    static final String STRANGER = "test-stranger";

    @Autowired
    SubscriptionMembershipRepository membershipRepository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    TestEntityManager entityManager;

    Subscription subscription;
    SubscriptionMembership ownerRow;
    SubscriptionMembership memberRow;

    @BeforeEach
    void setUp() {
        User owner = persistUser(OWNER);
        User member = persistUser(MEMBER);
        persistUser(STRANGER);
        Category gym = entityManager.persist(new Category("Test gym"));
        SubscriptionProvider provider = entityManager.persist(SubscriptionProvider.custom("My gym", gym, owner));
        LocalDate today = LocalDate.now();
        subscription = entityManager.persist(
                new Subscription(provider, new BigDecimal("35.00"), BillingCycle.MONTHLY, today.plusDays(10), false));
        ownerRow = entityManager.persist(new SubscriptionMembership(owner, subscription, MembershipRole.OWNER));
        memberRow = entityManager.persist(new SubscriptionMembership(member, subscription, MembershipRole.MEMBER));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findsTheOwnerRowWithUserAndSubscriptionLoaded() {
        SubscriptionMembership owner = membershipRepository
                .findBySubscriptionIdAndRole(subscription.getId(), MembershipRole.OWNER)
                .orElseThrow();

        assertThat(owner.getId()).isEqualTo(ownerRow.getId());
        PersistenceUnitUtil util =
                entityManager.getEntityManager().getEntityManagerFactory().getPersistenceUnitUtil();
        assertThat(util.isLoaded(owner, "user")).isTrue();
        assertThat(util.isLoaded(owner, "subscription")).isTrue();
        assertThat(util.isLoaded(owner.getSubscription(), "provider")).isTrue();
        assertThat(owner.getUser().getUsername()).isEqualTo(OWNER);
    }

    @Test
    void findsNoOwnerRowForUnknownSubscription() {
        assertThat(membershipRepository.findBySubscriptionIdAndRole(-1L, MembershipRole.OWNER))
                .isEmpty();
    }

    @Test
    void findsOnlyTheOwnerRowAfterThePaymentDate() {
        LocalDate paymentDate = subscription.getNextPaymentDate();

        assertThat(findOwnersDueBefore(paymentDate)).isEmpty();
        assertThat(findOwnersDueBefore(paymentDate.plusDays(1)))
                .extracting(SubscriptionMembership::getId)
                .containsExactly(ownerRow.getId());
    }

    @Test
    void ignoresCancelledSubscriptions() {
        Subscription cancelled =
                subscriptionRepository.findById(subscription.getId()).orElseThrow();
        cancelled.cancel(Instant.now());
        entityManager.flush();

        assertThat(findOwnersDueBefore(subscription.getNextPaymentDate().plusDays(1)))
                .isEmpty();
    }

    @Test
    void ignoresOwnersInOtherTimeZones() {
        LocalDate dayAfterPayment = subscription.getNextPaymentDate().plusDays(1);

        assertThat(findOwnersDueBefore(ZoneId.of("Asia/Tokyo"), dayAfterPayment))
                .isEmpty();
    }

    @Test
    void deletingSubscriptionDeletesItsMemberships() {
        subscriptionRepository.deleteById(subscription.getId());
        entityManager.flush();
        entityManager.clear();

        assertThat(membershipRepository.existsById(ownerRow.getId())).isFalse();
        assertThat(membershipRepository.existsById(memberRow.getId())).isFalse();
    }

    private List<SubscriptionMembership> findOwnersDueBefore(LocalDate date) {
        return findOwnersDueBefore(User.DEFAULT_TIME_ZONE, date);
    }

    private List<SubscriptionMembership> findOwnersDueBefore(ZoneId zone, LocalDate date) {
        return membershipRepository.findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                MembershipRole.OWNER, SubscriptionStatus.ACTIVE, zone, date);
    }

    private User persistUser(String username) {
        User user = TestUsers.user(username);
        return entityManager.persist(user);
    }
}
