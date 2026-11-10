package com.app.subly.repository;

import static com.app.subly.testdata.TestClock.DEFAULT_ZONE;
import static com.app.subly.testdata.TestClock.NOW;
import static com.app.subly.testdata.TestClock.TOKYO;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import com.app.subly.model.user.User;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionMembershipRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    SubscriptionMembershipRepository repository;

    @Autowired
    SubscriptionRepository subscriptionRepository;

    User alice;
    Subscription subscription;
    SubscriptionMembership ownerRow;
    SubscriptionMembership memberRow;

    @BeforeEach
    void setUp() {
        alice = persistUser(ALICE);
        User member = persistUser(BOB);
        subscription = persistSubscription(alice);
        ownerRow = entityManager.persist(SubscriptionMembership.owner(alice, subscription));
        memberRow = entityManager.persist(new SubscriptionMembership(member, subscription, MembershipRole.MEMBER));
        entityManager.flush();
    }

    @Test
    void findBySubscriptionIdAndRole_shouldReturnTheOwnerRow() {
        assertThat(repository.findBySubscriptionIdAndRole(subscription.getId(), MembershipRole.OWNER))
                .get()
                .extracting(SubscriptionMembership::getId)
                .isEqualTo(ownerRow.getId());
    }

    @Test
    void findWithSubscriptionDetailsBySubscriptionIdAndRole_shouldReturnTheOwnerRow() {
        assertThat(repository.findWithSubscriptionDetailsBySubscriptionIdAndRole(
                        subscription.getId(), MembershipRole.OWNER))
                .get()
                .extracting(SubscriptionMembership::getId)
                .isEqualTo(ownerRow.getId());
    }

    @Test
    void findOwnersDueBefore_shouldReturnOnlyTheOwnerRowAfterThePaymentDate() {
        LocalDate paymentDate = subscription.getNextPaymentDate();

        assertThat(findOwnersDueBefore(paymentDate)).isEmpty();
        assertThat(findOwnersDueBefore(paymentDate.plusDays(1)))
                .extracting(SubscriptionMembership::getId)
                .containsExactly(ownerRow.getId());
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
    void deleteById_shouldDeleteMembershipsOfTheSubscription() {
        subscriptionRepository.deleteById(subscription.getId());
        entityManager.flush();

        assertThat(repository.existsById(ownerRow.getId())).isFalse();
        assertThat(repository.existsById(memberRow.getId())).isFalse();
    }

    @Test
    void findCancelled_shouldIncludeMembersOfCancelledSharedSubscriptions() {
        subscription.cancel(NOW);
        entityManager.flush();

        assertThat(repository.findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
                        BOB, SubscriptionStatus.CANCELLED))
                .extracting(SubscriptionMembership::getId)
                .containsExactly(memberRow.getId());
    }

    private List<SubscriptionMembership> findOwnersDueBefore(LocalDate date) {
        return findOwnersDueBefore(DEFAULT_ZONE, date);
    }

    private List<SubscriptionMembership> findOwnersDueBefore(ZoneId zone, LocalDate date) {
        return repository.findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
                MembershipRole.OWNER, SubscriptionStatus.ACTIVE, zone, date);
    }
}
