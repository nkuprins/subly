package com.app.subly.repository;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.NOW;
import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestSubscriptions.aSubscription;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.subscription.SubscriptionPayment;
import com.app.subly.entity.user.User;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.SubscriptionStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionMembershipRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    SubscriptionMembershipRepository repository;

    User alice;
    Subscription subscription;
    SubscriptionMembership owner;
    SubscriptionMembership bobMember;

    @BeforeEach
    void setUp() {
        alice = persistUser(ALICE);
        User bob = persistUser(BOB);
        subscription = persistSubscription(alice);
        owner = entityManager.persist(SubscriptionMembership.owner(alice, subscription));
        bobMember = entityManager.persist(SubscriptionMembership.member(bob, subscription));
        entityManager.flush();
    }

    @Test
    void findBySubscriptionIdAndRole_shouldReturnTheOwnerRow() {
        assertThat(repository.findBySubscriptionIdAndRole(subscription.getId(), MembershipRole.OWNER))
                .get()
                .extracting(SubscriptionMembership::getId)
                .isEqualTo(owner.getId());
    }

    @Test
    void delete_shouldKeepPaymentsOfTheMember() {
        SubscriptionPayment payment = entityManager.persist(
                new SubscriptionPayment(subscription, bobMember.getUser(), new BigDecimal("5.00"), TODAY));
        entityManager.flush();

        repository.delete(bobMember);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(SubscriptionPayment.class, payment.getId()))
                .isNotNull();
    }

    @Test
    void findOwnersDueBefore_shouldReturnOnlyTheOwnerRowAfterThePaymentDate() {
        LocalDate paymentDate = subscription.getNextPaymentDate();

        assertThat(findOwnersDueBefore(paymentDate)).isEmpty();
        assertThat(findOwnersDueBefore(paymentDate.plusDays(1)))
                .extracting(SubscriptionMembership::getId)
                .containsExactly(owner.getId());
    }

    @Test
    void findOwnersDueBefore_shouldIgnoreCancelledSubscriptions() {
        subscription.cancel(NOW);
        entityManager.flush();

        assertThat(findOwnersDueBefore(subscription.getNextPaymentDate().plusDays(1)))
                .isEmpty();
    }

    @Test
    void findOwnersDueBefore_shouldIgnoreOwnersInOtherTimeZones() {
        LocalDate dayAfterPayment = subscription.getNextPaymentDate().plusDays(1);

        assertThat(findOwnersDueBefore(TOKYO, dayAfterPayment)).isEmpty();
    }

    @Test
    void findMembershipsOf_shouldIncludeMembersOfCancelledSharedSubscriptions() {
        subscription.cancel(NOW);
        entityManager.flush();

        assertThat(repository.findMembershipsOf(BOB, SubscriptionStatus.CANCELLED))
                .extracting(SubscriptionMembership::getId)
                .containsExactly(bobMember.getId());
    }

    @Test
    void findMembershipsOf_shouldOrderByNextPaymentDate() {
        Subscription sooner = entityManager.persist(aSubscription()
                .withProvider(subscription.getProvider())
                .withNextPaymentDate(subscription.getNextPaymentDate().minusDays(5))
                .build());
        SubscriptionMembership soonerOwner = entityManager.persist(SubscriptionMembership.owner(alice, sooner));
        entityManager.flush();

        assertThat(repository.findMembershipsOf(ALICE, SubscriptionStatus.ACTIVE))
                .extracting(SubscriptionMembership::getId)
                .containsExactly(soonerOwner.getId(), owner.getId());
    }

    private List<SubscriptionMembership> findOwnersDueBefore(LocalDate date) {
        return findOwnersDueBefore(DEFAULT_ZONE, date);
    }

    private List<SubscriptionMembership> findOwnersDueBefore(ZoneId zone, LocalDate date) {
        return repository.findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                MembershipRole.OWNER, SubscriptionStatus.ACTIVE, zone, date);
    }
}
