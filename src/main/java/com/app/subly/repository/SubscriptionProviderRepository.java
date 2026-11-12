package com.app.subly.repository;

import com.app.subly.dto.provider.ProviderCategoryResponse;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.enums.ProviderType;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SubscriptionProviderRepository extends JpaRepository<SubscriptionProvider, Long> {

    @EntityGraph(attributePaths = "category")
    List<SubscriptionProvider> findAllByProviderTypeOrCreatedByUsernameOrderByNameAsc(
            ProviderType providerType, String username);

    @EntityGraph(attributePaths = "category")
    List<SubscriptionProvider> findAllByProviderTypeOrderByNameAsc(ProviderType providerType);

    //  SELECT COUNT(*), categories.name FROM subscription_providers JOIN categories ON categories.id = category_id
    // GROUP
    // BY category_id, name;
    @Query("""
            SELECT new com.app.subly.dto.provider.ProviderCategoryResponse(
                       c.id, COUNT(sp)
            ) FROM SubscriptionProvider sp
              JOIN sp.category c
              GROUP BY c.id

            """)
    List<ProviderCategoryResponse> getAllProvidersCountGroupedByCategory();
}
