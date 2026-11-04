package com.app.subly.repository;

import com.app.subly.models.subscription.Subscription;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    @EntityGraph(attributePaths = {"provider", "provider.category"})
    Optional<Subscription> findWithProviderById(Long id);
}
