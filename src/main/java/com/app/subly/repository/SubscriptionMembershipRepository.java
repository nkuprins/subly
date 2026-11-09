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

    @EntityGraph(attributePaths = {"user", "subscription.provider.category"})
    Optional<SubscriptionMembership> findWithSubscriptionDetailsBySubscriptionIdAndRole(
            Long subscriptionId, MembershipRole role);

    @EntityGraph(attributePaths = "user")
    List<SubscriptionMembership> findBySubscriptionIdInAndRole(Collection<Long> subscriptionIds, MembershipRole role);

    List<SubscriptionMembership> findBySubscriptionIdIn(Collection<Long> subscriptionIds);

    @EntityGraph(attributePaths = {"subscription", "subscription.provider"})
    List<SubscriptionMembership> findByUserUsernameOrderBySubscriptionNextPaymentDateAsc(String username);

    @EntityGraph(attributePaths = {"subscription", "subscription.provider"})
    List<SubscriptionMembership> findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
            String username, SubscriptionStatus status);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionMembership> findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
            MembershipRole role, SubscriptionStatus status, ZoneId timeZone, LocalDate date);

    long countBySubscriptionId(Long subscriptionId);
}
