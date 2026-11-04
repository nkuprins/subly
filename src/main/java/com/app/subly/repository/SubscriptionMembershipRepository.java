package com.app.subly.repository;

import com.app.subly.models.subscription.MembershipRole;
import com.app.subly.models.subscription.SubscriptionMembership;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubscriptionMembershipRepository extends JpaRepository<SubscriptionMembership, Long> {

    boolean existsBySubscriptionIdAndUserUsernameAndRole(Long subscriptionId, String username, MembershipRole role);

    @EntityGraph(attributePaths = "subscription.service.category")
    List<SubscriptionMembership> findByUserUsernameAndRoleOrderBySubscriptionNextPaymentDate(
            String username, MembershipRole role);
}
