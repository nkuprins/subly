package com.app.subly.service;

import static com.app.subly.testdata.TestProviders.CATALOG_PROVIDER_NAME;
import static com.app.subly.testdata.TestProviders.CUSTOM_PROVIDER_NAME;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.dto.provider.ProviderCategoryResponse;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.entity.category.Category;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.user.User;
import com.app.subly.enums.ProviderType;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    private static final long CATEGORY_ID = 4L;
    private static final long PROVIDER_ID = 3L;
    private static final String LOGO_PATH = "/logo.svg";

    @Mock
    private SubscriptionProviderRepository subscriptionProviderRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProviderService service;

    @Captor
    private ArgumentCaptor<SubscriptionProvider> providerCaptor;

    private final Category category = TestProviders.category();
    private final SubscriptionProvider catalog = TestProviders.catalog(category);

    @Test
    void findCatalogProviders_shouldReturnCatalogProviders() {
        when(subscriptionProviderRepository.findByProviderTypeOrderByNameAsc(ProviderType.CATALOG))
                .thenReturn(List.of(catalog));

        assertThat(service.findCatalogProviders()).containsExactly(catalog);
    }

    @Test
    void findVisibleProviders_shouldMapProvidersToResponses() {
        catalog.setId(PROVIDER_ID);
        catalog.setLogoPath(LOGO_PATH);
        when(subscriptionProviderRepository.findByProviderTypeOrCreatedByUsernameOrderByNameAsc(
                        ProviderType.CATALOG, ALICE))
                .thenReturn(List.of(catalog));

        assertThat(service.findVisibleProviders(ALICE))
                .containsExactly(
                        new ProviderResponse(PROVIDER_ID, CATALOG_PROVIDER_NAME, LOGO_PATH, ProviderType.CATALOG));
    }

    @Test
    void deleteAllProvidersCreatedBy_shouldDeleteProvidersOfTheUser() {
        service.deleteAllProvidersCreatedBy(ALICE);

        verify(subscriptionProviderRepository).deleteByCreatedByUsername(ALICE);
    }

    @Test
    void findProviderCountsByCategory_shouldReturnCountsPerCategory() {
        ProviderCategoryResponse count = new ProviderCategoryResponse(CATEGORY_ID, 2L);
        when(subscriptionProviderRepository.findProviderCountsByCategory()).thenReturn(List.of(count));

        assertThat(service.findProviderCountsByCategory()).containsExactly(count);
    }

    @Test
    void create_shouldSaveCatalogProviderWithoutCreator() {
        stubCategory();

        service.create(
                ALICE,
                new CreateProviderForm(padded(CATALOG_PROVIDER_NAME), CATEGORY_ID, padded(LOGO_PATH)),
                ProviderType.CATALOG);

        SubscriptionProvider saved = savedProvider();
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.CATALOG);
        assertThat(saved.getCreatedBy()).isNull();
        assertThat(saved.getName()).isEqualTo(CATALOG_PROVIDER_NAME);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getLogoPath()).isEqualTo(LOGO_PATH);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "   ")
    void create_shouldSaveCustomProviderOwnedByTheUser(String logoPath) {
        User alice = stubAliceExists();
        stubCategory();

        service.create(
                ALICE,
                new CreateProviderForm(padded(CUSTOM_PROVIDER_NAME), CATEGORY_ID, logoPath),
                ProviderType.CUSTOM);

        SubscriptionProvider saved = savedProvider();
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.CUSTOM);
        assertThat(saved.getCreatedBy()).isSameAs(alice);
        assertThat(saved.getName()).isEqualTo(CUSTOM_PROVIDER_NAME);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getLogoPath()).isNull();
    }

    private void stubCategory() {
        when(categoryRepository.getCategory(CATEGORY_ID)).thenReturn(category);
    }

    private SubscriptionProvider savedProvider() {
        verify(subscriptionProviderRepository).save(providerCaptor.capture());
        return providerCaptor.getValue();
    }

    private User stubAliceExists() {
        User alice = TestUsers.user(ALICE);
        when(userRepository.getUser(ALICE)).thenReturn(alice);
        return alice;
    }

    private static String padded(String value) {
        return "  " + value + "  ";
    }
}
