package com.app.subly.repository;

import com.app.subly.model.subscription.Subscription;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Subscription> findLockedById(Long id);
}
