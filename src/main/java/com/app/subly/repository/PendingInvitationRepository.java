package com.app.subly.repository;

import com.app.subly.models.subscription.PendingInvitation;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PendingInvitationRepository extends JpaRepository<PendingInvitation, Long> {

    boolean existsBySubscriptionIdAndUserId(Long subscriptionId, Long userId);

    @EntityGraph(attributePaths = "user")
    List<PendingInvitation> findBySubscriptionIdOrderByInvitedAt(Long subscriptionId);

    @EntityGraph(attributePaths = "subscription.service")
    List<PendingInvitation> findByUserUsernameOrderByInvitedAt(String username);

    @EntityGraph(attributePaths = {"user", "subscription"})
    Optional<PendingInvitation> findByIdAndUserUsername(Long id, String username);
}
