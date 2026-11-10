package com.app.subly.repository;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionPayment;
import com.app.subly.model.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionPaymentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    SubscriptionPaymentRepository repository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    @Autowired
    SubscriptionMembershipRepository membershipRepository;

    User alice;
    User bob;
    Subscription subscription;
    SubscriptionMembership bobRow;

    @BeforeEach
    void setUp() {
        alice = persistUser(ALICE);
        bob = persistUser(BOB);
        subscription = persistSubscription(alice);
        entityManager.persist(SubscriptionMembership.owner(alice, subscription));
        bobRow = entityManager.persist(SubscriptionMembership.member(bob, subscription));
    }

    @Test
    void findHistoryOf_shouldReturnOnlyTheUsersPaymentsNewestFirst() {
        SubscriptionPayment older = persistPayment(alice, TODAY.minusMonths(1));
        SubscriptionPayment newer = persistPayment(alice, TODAY);
        persistPayment(bob, TODAY);

        assertThat(repository.findHistoryOf(ALICE))
                .extracting(SubscriptionPayment::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findPaidSince_shouldIncludeTheStartDateButNotTheDayBefore() {
        LocalDate from = TODAY.withDayOfMonth(1);
        SubscriptionPayment onStart = persistPayment(alice, from);
        persistPayment(alice, from.minusDays(1));

        assertThat(repository.findPaidSince(ALICE, from))
                .extracting(SubscriptionPayment::getId)
                .containsExactly(onStart.getId());
    }

    @Test
    void deleteById_shouldDeletePaymentsOfTheSubscription() {
        SubscriptionPayment payment = persistPayment(alice, TODAY);
        entityManager.flush();

        subscriptionRepository.deleteById(subscription.getId());
        entityManager.flush();

        assertThat(repository.existsById(payment.getId())).isFalse();
    }

    @Test
    void deletingAMembership_shouldKeepTheMembersPayments() {
        SubscriptionPayment payment = persistPayment(bob, TODAY);
        entityManager.flush();

        membershipRepository.deleteById(bobRow.getId());
        entityManager.flush();

        assertThat(repository.existsById(payment.getId())).isTrue();
    }

    private SubscriptionPayment persistPayment(User user, LocalDate paidOn) {
        return entityManager.persist(new SubscriptionPayment(subscription, user, new BigDecimal("5.00"), paidOn));
    }
}
