package com.app.subly.repository;

import com.app.subly.model.subscription.MembershipRole;
import com.app.subly.model.subscription.SubscriptionMembership;
import com.app.subly.model.subscription.SubscriptionStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionMembershipRepository extends JpaRepository<SubscriptionMembership, Long> {

    boolean existsBySubscriptionIdAndUserId(Long subscriptionId, Long userId);

    @EntityGraph(attributePaths = {"user", "subscription"})
    Optional<SubscriptionMembership> findBySubscriptionIdAndRole(Long subscriptionId, MembershipRole role);

    List<SubscriptionMembership> findBySubscriptionIdIn(Collection<Long> subscriptionIds);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionMembership> findByUser_IdAndRole(Long userId, MembershipRole role);

    @EntityGraph(attributePaths = {"subscription", "subscription.provider"})
    List<SubscriptionMembership> findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
            String username, SubscriptionStatus status);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionMembership> findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
            MembershipRole role, SubscriptionStatus status, ZoneId timeZone, LocalDate date);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionMembership> findBySubscriptionStatus(SubscriptionStatus status);

    void deleteByUser_Id(Long userId);

    @EntityGraph(attributePaths = "user")
    List<SubscriptionMembership> findBySubscriptionIdOrderByIdAsc(Long subscriptionId);

    Optional<SubscriptionMembership> findBySubscriptionIdAndUserUsername(Long subscriptionId, String username);

    Optional<SubscriptionMembership> findByIdAndSubscriptionId(Long id, Long subscriptionId);

    long countBySubscriptionId(Long subscriptionId);
}
