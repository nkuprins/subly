package com.app.subly.repository;

import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.SubscriptionProvider;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionProviderRepository extends JpaRepository<SubscriptionProvider, Long> {

    @EntityGraph(attributePaths = "category")
    List<SubscriptionProvider> findAllByProviderTypeOrCreatedByUsernameOrderByNameAsc(
            ProviderType providerType, String username);

    Optional<SubscriptionProvider> findByIdAndCreatedByUsername(long id, String username);

    @EntityGraph(attributePaths = "category")
    List<SubscriptionProvider> findAllByProviderTypeOrderByNameAsc(ProviderType providerType);
}
