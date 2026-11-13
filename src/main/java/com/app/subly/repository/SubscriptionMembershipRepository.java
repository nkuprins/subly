package com.app.subly.repository;

import com.app.subly.entity.subscription.SubscriptionMembership;
import com.app.subly.enums.MembershipRole;
import com.app.subly.enums.SubscriptionStatus;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionMembershipRepository extends JpaRepository<SubscriptionMembership, Long> {

    boolean existsBySubscriptionIdAndUserId(long subscriptionId, long userId);

    @EntityGraph(attributePaths = {"user", "subscription"})
    Optional<SubscriptionMembership> findBySubscriptionIdAndRole(long subscriptionId, MembershipRole role);

    List<SubscriptionMembership> findBySubscriptionIdIn(Collection<Long> subscriptionIds);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionMembership> findByUserIdAndRole(long userId, MembershipRole role);

    default List<SubscriptionMembership> findMembershipsOf(String username, SubscriptionStatus status) {
        return findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(username, status);
    }

    @EntityGraph(attributePaths = {"subscription", "subscription.provider"})
    List<SubscriptionMembership> findByUserUsernameAndSubscriptionStatusOrderBySubscriptionNextPaymentDateAsc(
            String username, SubscriptionStatus status);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionMembership> findByRoleAndSubscriptionStatusAndUserTimeZoneAndSubscriptionNextPaymentDateBefore(
            MembershipRole role, SubscriptionStatus status, ZoneId timeZone, LocalDate date);

    @EntityGraph(attributePaths = "subscription")
    List<SubscriptionMembership> findBySubscriptionStatus(SubscriptionStatus status);

    void deleteByUserId(long userId);

    @EntityGraph(attributePaths = "user")
    List<SubscriptionMembership> findBySubscriptionIdOrderByIdAsc(long subscriptionId);

    Optional<SubscriptionMembership> findBySubscriptionIdAndUserUsername(long subscriptionId, String username);

    Optional<SubscriptionMembership> findByIdAndSubscriptionId(long id, long subscriptionId);

    long countBySubscriptionId(long subscriptionId);
}
