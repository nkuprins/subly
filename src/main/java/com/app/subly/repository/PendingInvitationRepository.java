package com.app.subly.repository;

import com.app.subly.exception.invitation.InvitationNotFoundException;
import com.app.subly.model.subscription.PendingInvitation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingInvitationRepository extends JpaRepository<PendingInvitation, Long> {

    boolean existsBySubscriptionIdAndUserId(Long subscriptionId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<PendingInvitation> findBySubscriptionIdOrderByInvitedAt(Long subscriptionId);

    @EntityGraph(attributePaths = "subscription.provider")
    List<PendingInvitation> findByUserUsernameOrderByInvitedAt(String username);

    @EntityGraph(attributePaths = {"user", "subscription"})
    Optional<PendingInvitation> findByIdAndUserUsername(Long id, String username);

    default PendingInvitation getByIdAndUserUsername(long id, String username) {
        return findByIdAndUserUsername(id, username).orElseThrow(() -> new InvitationNotFoundException(id));
    }
}
