package com.app.subly.repository;

import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.SubscriptionMembership;
import com.app.subly.models.subscription.SubscriptionStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface SubscriptionMembershipRepository extends JpaRepository<SubscriptionMembership, Long> {

    boolean existsBySubscriptionIdAndUserUsernameAndRole(Long subscriptionId, String username, MembershipRole role);

    @EntityGraph(attributePaths = "subscription.provider.category")
    List<SubscriptionMembership> findByUserUsernameAndRoleOrderBySubscriptionNextPaymentDate(
            String username, MembershipRole role);

    @EntityGraph(attributePaths = {"subscription", "subscription.provider"})
    List<SubscriptionMembership> findByUser_UsernameAndSubscription_StatusOrderBySubscription_NextPaymentDateAsc(
            String username,
            SubscriptionStatus status);

    List<SubscriptionMembership> findBySubscription_IdIn(
            Collection<Long> subscriptionIds);
}
