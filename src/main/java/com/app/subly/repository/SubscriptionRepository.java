package com.app.subly.repository;

import com.app.subly.models.subscription.Subscription;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Subscription> findLockedById(Long id);
}
