package com.app.subly.repository;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;

import com.app.subly.model.category.Category;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionProviderRepositoryTest extends AbstractRepositoryTest {

    static final String OWN_CUSTOM_NAME = "A own";
    static final String CATALOG_NAME = "B catalog";
    static final String OTHERS_CUSTOM_NAME = "C other";

    @Autowired
    SubscriptionProviderRepository repository;

    SubscriptionProvider catalog;
    SubscriptionProvider aliceCustom;
    SubscriptionProvider bobCustom;

    @BeforeEach
    void setUp() {
        Category category = persistCategory();
        User alice = persistUser(ALICE);
        User bob = persistUser(BOB);
        catalog = entityManager.persist(SubscriptionProvider.catalog(CATALOG_NAME, category));
        aliceCustom = entityManager.persist(SubscriptionProvider.custom(OWN_CUSTOM_NAME, category, alice));
        bobCustom = entityManager.persist(SubscriptionProvider.custom(OTHERS_CUSTOM_NAME, category, bob));
        entityManager.flush();
    }

    @Test
    void findAllByProviderTypeOrCreatedByUsernameOrderByNameAsc_shouldReturnCatalogAndOwnProvidersByName() {
        assertThat(repository.findAllByProviderTypeOrCreatedByUsernameOrderByNameAsc(ProviderType.CATALOG, ALICE))
                .extracting(SubscriptionProvider::getName)
                .containsExactly(OWN_CUSTOM_NAME, CATALOG_NAME);
    }

    @Test
    void findByIdAndCreatedByUsername_shouldFindOnlyOwnCustomProvider() {
        assertThat(repository.findByIdAndCreatedByUsername(aliceCustom.getId(), ALICE))
                .isPresent();
        assertThat(repository.findByIdAndCreatedByUsername(catalog.getId(), ALICE))
                .isEmpty();
        assertThat(repository.findByIdAndCreatedByUsername(bobCustom.getId(), ALICE))
                .isEmpty();
    }
}
