package com.app.subly.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.exception.invitation.InvitationNotFoundException;
import com.app.subly.model.TestUsers;
import com.app.subly.model.User;
import com.app.subly.model.subscription.BillingCycle;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.PendingInvitation;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.subscription.SubscriptionProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
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
class PendingInvitationRepositoryTest {

    static final String OWNER = "test-owner";
    static final String INVITEE = "test-invitee";

    @Autowired
    PendingInvitationRepository invitationRepository;

    @Autowired
    TestEntityManager entityManager;

    PendingInvitation invitation;

    @BeforeEach
    void setUp() {
        User owner = entityManager.persist(TestUsers.user(OWNER));
        User invitee = entityManager.persist(TestUsers.user(INVITEE));
        Category gym = entityManager.persist(new Category("Test gym"));
        SubscriptionProvider provider = entityManager.persist(SubscriptionProvider.custom("My gym", gym, owner));
        Subscription subscription = entityManager.persist(new Subscription(
                provider,
                new BigDecimal("35.00"),
                BillingCycle.MONTHLY,
                LocalDate.now().plusDays(10),
                false));
        invitation = entityManager.persist(new PendingInvitation(invitee, subscription));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void getsInvitationOfTheInvitedUser() {
        PendingInvitation found = invitationRepository.getByIdAndUserUsername(invitation.getId(), INVITEE);

        assertThat(found.getId()).isEqualTo(invitation.getId());
    }

    @Test
    void throwsForInvitationOfAnotherUser() {
        assertThatThrownBy(() -> invitationRepository.getByIdAndUserUsername(invitation.getId(), OWNER))
                .isInstanceOf(InvitationNotFoundException.class)
                .hasMessageContaining(String.valueOf(invitation.getId()));
    }

    @Test
    void throwsForUnknownInvitation() {
        long unknownId = invitation.getId() + 1;

        assertThatThrownBy(() -> invitationRepository.getByIdAndUserUsername(unknownId, INVITEE))
                .isInstanceOf(InvitationNotFoundException.class)
                .hasMessageContaining(String.valueOf(unknownId));
    }
}
