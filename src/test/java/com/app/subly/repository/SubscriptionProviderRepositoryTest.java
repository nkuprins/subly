package com.app.subly.repository;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.app.subly.entity.category.Category;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.user.User;
import com.app.subly.enums.ProviderType;
import com.app.subly.exception.provider.ProviderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionProviderRepositoryTest extends AbstractRepositoryTest {

    private static final String OWN_CUSTOM_NAME = "A own";
    private static final String CATALOG_NAME = "B catalog";
    private static final String OTHERS_CUSTOM_NAME = "C other";

    private static final long UNKNOWN_PROVIDER_ID = -1L;

    @Autowired
    private SubscriptionProviderRepository repository;

    private SubscriptionProvider catalog;

    @BeforeEach
    void setUp() {
        Category category = persistCategory();
        User alice = persistUser(ALICE);
        User bob = persistUser(BOB);
        catalog = entityManager.persist(SubscriptionProvider.catalog(CATALOG_NAME, category));
        entityManager.persist(SubscriptionProvider.custom(OWN_CUSTOM_NAME, category, alice));
        entityManager.persist(SubscriptionProvider.custom(OTHERS_CUSTOM_NAME, category, bob));
        entityManager.flush();
    }

    @Test
    void findByProviderTypeOrCreatedByUsernameOrderByNameAsc_shouldReturnCatalogAndOwnProvidersByName() {
        assertThat(repository.findByProviderTypeOrCreatedByUsernameOrderByNameAsc(ProviderType.CATALOG, ALICE))
                .extracting(SubscriptionProvider::getName)
                .containsExactly(OWN_CUSTOM_NAME, CATALOG_NAME);
    }

    @Test
    void getProvider_shouldReturnProvider() {
        assertThat(repository.getProvider(catalog.getId())).isSameAs(catalog);
    }

    @Test
    void getProvider_shouldRejectUnknownProvider() {
        assertThatThrownBy(() -> repository.getProvider(UNKNOWN_PROVIDER_ID))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining(String.valueOf(UNKNOWN_PROVIDER_ID));
    }

    @Test
    void deleteByCreatedByUsername_shouldDeleteOnlyProvidersOfThatUser() {
        repository.deleteByCreatedByUsername(ALICE);
        entityManager.flush();

        assertThat(repository.findAll())
                .extracting(SubscriptionProvider::getName)
                .containsExactlyInAnyOrder(CATALOG_NAME, OTHERS_CUSTOM_NAME);
    }
}
