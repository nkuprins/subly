package com.app.subly.repository;

import static com.app.subly.testdata.TestClock.TODAY;
import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.entity.subscription.SubscriptionPayment;
import com.app.subly.entity.user.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionPaymentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private SubscriptionPaymentRepository repository;

    private User alice;
    private User bob;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        alice = persistUser(ALICE);
        bob = persistUser(BOB);
        subscription = persistSubscription(alice);
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

    private SubscriptionPayment persistPayment(User user, LocalDate paidOn) {
        return entityManager.persist(new SubscriptionPayment(subscription, user, new BigDecimal("5.00"), paidOn));
    }
}
