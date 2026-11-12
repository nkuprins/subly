package com.app.subly.repository;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.entity.invitation.PendingInvitation;
import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.entity.subscription.SubscriptionPayment;
import com.app.subly.entity.user.User;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    SubscriptionRepository repository;

    @Test
    void delete_shouldDeleteMembershipsInvitationsAndPayments() {
        User alice = persistUser(ALICE);
        User bob = persistUser(BOB);
        Subscription subscription = persistSubscription(alice);
        SubscriptionMembership owner = entityManager.persist(SubscriptionMembership.owner(alice, subscription));
        PendingInvitation invitation = entityManager.persist(new PendingInvitation(bob, subscription, alice));
        SubscriptionPayment payment =
                entityManager.persist(new SubscriptionPayment(subscription, alice, new BigDecimal("5.00"), TODAY));
        entityManager.flush();

        repository.delete(subscription);
        entityManager.flush();
        entityManager.clear();

        assertThat(entityManager.find(SubscriptionMembership.class, owner.getId()))
                .isNull();
        assertThat(entityManager.find(PendingInvitation.class, invitation.getId()))
                .isNull();
        assertThat(entityManager.find(SubscriptionPayment.class, payment.getId()))
                .isNull();
    }
}
