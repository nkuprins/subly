package com.app.subly.repository;

import com.app.subly.entity.invitation.PendingInvitation;
import com.app.subly.exception.invitation.InvitationNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingInvitationRepository extends JpaRepository<PendingInvitation, Long> {

    boolean existsBySubscriptionIdAndUserId(long subscriptionId, long userId);

    @EntityGraph(attributePaths = "user")
    List<PendingInvitation> findBySubscriptionIdOrderByInvitedAtAsc(long subscriptionId);

    @EntityGraph(attributePaths = {"subscription.provider", "invitedBy"})
    List<PendingInvitation> findByUserUsernameOrderByInvitedAtAsc(String username);

    @EntityGraph(attributePaths = {"user", "subscription"})
    Optional<PendingInvitation> findByIdAndUserUsername(long id, String username);

    void deleteByUserId(long userId);

    default PendingInvitation getInvitation(long id, String username) {
        return findByIdAndUserUsername(id, username).orElseThrow(() -> new InvitationNotFoundException(id));
    }
}
