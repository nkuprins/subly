package com.app.subly.repository;

import com.app.subly.models.subscription.SubscriptionProvider;
import com.app.subly.models.subscription.ProviderType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubscriptionProviderRepository extends JpaRepository<SubscriptionProvider, Long> {

    Optional<SubscriptionProvider> findByIdAndProviderType(Long id, ProviderType providerType);
}
