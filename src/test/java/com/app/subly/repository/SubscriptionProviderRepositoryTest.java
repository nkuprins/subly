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

    SubscriptionProvider catalog;
    SubscriptionProvider ownCustom;
    SubscriptionProvider othersCustom;

    @BeforeEach
    void setUp() {
        Category gym = entityManager.persist(new Category("Test gym"));
        User owner = persistUser("test-owner");
        User other = persistUser("test-other");
        catalog = entityManager.persist(SubscriptionProvider.catalog("B catalog", gym));
        ownCustom = entityManager.persist(SubscriptionProvider.custom("A own", gym, owner));
        othersCustom = entityManager.persist(SubscriptionProvider.custom("C other", gym, other));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void findsCatalogAndOwnProvidersByName() {
        assertThat(subscriptionProviderRepository.findAllByProviderTypeOrCreatedBy_UsernameOrderByNameAsc(
                        ProviderType.CATALOG, "test-owner"))
                .extracting(SubscriptionProvider::getName)
                .containsExactly("A own", "B catalog");
    }

    @Test
    void findByIdAndCreatedByUsernameSkipsCatalogAndOthersProviders() {
        assertThat(subscriptionProviderRepository.findByIdAndCreatedBy_Username(ownCustom.getId(), "test-owner"))
                .isPresent();
        assertThat(subscriptionProviderRepository.findByIdAndCreatedBy_Username(catalog.getId(), "test-owner"))
                .isEmpty();
        assertThat(subscriptionProviderRepository.findByIdAndCreatedBy_Username(othersCustom.getId(), "test-owner"))
                .isEmpty();
    }

    private User persistUser(String username) {
        User user = new User();
        user.setUsername(username);
        user.setName("Test User");
        user.setEmail(username + "@test.local");
        user.setPasswordHash("not-a-real-hash");
        user.setRole(UserRole.USER);
        return entityManager.persist(user);
    }
}
