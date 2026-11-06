package com.app.subly.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.model.User;
import com.app.subly.model.UserRole;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.SubscriptionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class SubscriptionProviderRepositoryTest {

    @Autowired
    SubscriptionProviderRepository subscriptionProviderRepository;

    @Autowired
    TestEntityManager entityManager;

    Category gym;

    @BeforeEach
    void setUp() {
        gym = entityManager.persist(new Category("Test gym"));
    }

    @Test
    void findByIdAndProviderTypeReturnsCatalogProvider() {
        SubscriptionProvider catalog =
                entityManager.persistFlushFind(SubscriptionProvider.catalog("Test catalog", gym));

        assertThat(subscriptionProviderRepository.findByIdAndProviderType(catalog.getId(), ProviderType.CATALOG))
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
        SubscriptionProvider custom =
                entityManager.persistFlushFind(SubscriptionProvider.custom("My gym", gym, creator));

        assertThat(subscriptionProviderRepository.findByIdAndProviderType(custom.getId(), ProviderType.CATALOG))
                .isEmpty();
    }
}
