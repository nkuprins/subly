package com.app.subly.repository;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.exception.invitation.InvitationNotFoundException;
import com.app.subly.model.invitation.PendingInvitation;
import com.app.subly.model.subscription.Subscription;
import com.app.subly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PendingInvitationRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    PendingInvitationRepository invitationRepository;

    PendingInvitation invitation;

    @BeforeEach
    void setUp() {
        User alice = persistUser(ALICE);
        User bob = persistUser(BOB);
        Subscription subscription = persistSubscription(alice);
        invitation = entityManager.persist(new PendingInvitation(bob, subscription, alice));
        entityManager.flush();
    }

    @Test
    void getByIdAndUserUsername_shouldReturnInvitationOfTheInvitedUser() {
        PendingInvitation found = invitationRepository.getByIdAndUserUsername(invitation.getId(), BOB);

        assertThat(found.getId()).isEqualTo(invitation.getId());
    }

    @Test
    void getByIdAndUserUsername_shouldRejectInvitationOfAnotherUser() {
        long invitationId = invitation.getId();

        assertThatThrownBy(() -> invitationRepository.getByIdAndUserUsername(invitationId, ALICE))
                .isInstanceOf(InvitationNotFoundException.class)
                .hasMessageContaining(String.valueOf(invitationId));
    }

    @Test
    void getByIdAndUserUsername_shouldRejectUnknownInvitation() {
        long unknownId = invitation.getId() + 1;

        assertThatThrownBy(() -> invitationRepository.getByIdAndUserUsername(unknownId, BOB))
                .isInstanceOf(InvitationNotFoundException.class)
                .hasMessageContaining(String.valueOf(unknownId));
    }
}
