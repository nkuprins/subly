package com.app.subly.repository;

import com.app.subly.models.User;
import com.app.subly.models.UserRole;
import com.app.subly.models.subscription.Category;
import com.app.subly.models.subscription.SubscriptionProvider;
import com.app.subly.models.subscription.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionProviderRepositoryTest {

    @Autowired
    SubscriptionProviderRepository providerRepository;
    @Autowired TestEntityManager entityManager;

    Category gym;

    @BeforeEach
    void setUp() {
        gym = entityManager.persist(new Category("Test gym"));
    }

    @Test
    void findByIdAndProviderTypeReturnsCatalogProvider() {
        SubscriptionProvider catalog = entityManager.persistFlushFind(SubscriptionProvider.catalog("Test catalog", gym));

        assertThat(providerRepository.findByIdAndProviderType(catalog.getId(), ProviderType.CATALOG))
                .map(SubscriptionProvider::getName)
                .contains("Test catalog");
    }

    @Test
    void findByIdAndProviderTypeSkipsCustomProviderWhenAskedForCatalog() {
        User creator = new User();
        creator.setUsername("test-owner");
        creator.setName("Test Owner");
        creator.setEmail("test-owner@test.local");
        creator.setPasswordHash("not-a-real-hash");
        creator.setRole(UserRole.USER);
        entityManager.persist(creator);
        SubscriptionProvider custom = entityManager.persistFlushFind(SubscriptionProvider.custom("My gym", gym, creator));

        assertThat(providerRepository.findByIdAndProviderType(custom.getId(), ProviderType.CATALOG))
                .isEmpty();
    }
}
