package com.app.subly.repository;

import com.app.subly.entity.subscription.Subscription;
import com.app.subly.enums.ProviderType;
import com.app.subly.enums.SubscriptionStatus;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Subscription> findLockedById(long id);

    long countByStatus(SubscriptionStatus status);

    @EntityGraph(attributePaths = "provider")
    List<Subscription> findByStatusAndProviderProviderType(SubscriptionStatus status, ProviderType providerType);
}
