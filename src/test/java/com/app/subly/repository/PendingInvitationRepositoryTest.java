package com.app.subly.repository;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.entity.invitation.PendingInvitation;
import com.app.subly.entity.user.User;
import com.app.subly.exception.invitation.InvitationNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PendingInvitationRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private PendingInvitationRepository invitationRepository;

    private PendingInvitation invitation;

    @BeforeEach
    void setUp() {
        User alice = persistUser(ALICE);
        User bob = persistUser(BOB);
        invitation = entityManager.persist(new PendingInvitation(bob, persistSubscription(alice), alice));
        entityManager.flush();
    }

    @Test
    void getInvitation_shouldReturnInvitationOfTheInvitedUser() {
        PendingInvitation found = invitationRepository.getInvitation(invitation.getId(), BOB);

        assertThat(found.getId()).isEqualTo(invitation.getId());
    }

    @Test
    void getInvitation_shouldRejectInvitationOfAnotherUser() {
        long invitationId = invitation.getId();

        assertThatThrownBy(() -> invitationRepository.getInvitation(invitationId, ALICE))
                .isInstanceOf(InvitationNotFoundException.class)
                .hasMessageContaining(String.valueOf(invitationId));
    }
}
