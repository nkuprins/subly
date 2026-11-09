package com.app.subly.service;

import static com.app.subly.testdata.TestProviders.CATALOG_PROVIDER_NAME;
import static com.app.subly.testdata.TestProviders.CUSTOM_PROVIDER_NAME;
import static com.app.subly.testdata.TestUsers.ALICE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.exception.category.InvalidCategoryException;
import com.app.subly.exception.provider.ProviderNotFoundException;
import com.app.subly.model.category.Category;
import com.app.subly.model.provider.ProviderType;
import com.app.subly.model.provider.SubscriptionProvider;
import com.app.subly.model.user.User;
import com.app.subly.model.user.UserRole;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    static final long CATEGORY_ID = 4L;
    static final long OTHER_CATEGORY_ID = 5L;
    static final long PROVIDER_ID = 3L;
    static final String LOGO_PATH = "/logo.svg";
    static final String NEW_NAME = "Renamed";
    static final String NEW_LOGO_PATH = "/new.svg";
    static final ProviderRequest UPDATE_REQUEST =
            new ProviderRequest(padded(NEW_NAME), OTHER_CATEGORY_ID, NEW_LOGO_PATH);

    @Mock
    SubscriptionProviderRepository subscriptionProviderRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProviderService service;

    @Captor
    ArgumentCaptor<SubscriptionProvider> providerCaptor;

    final Category category = TestProviders.category();
    final Category otherCategory = new Category("Test otherCategory");

    @Test
    void create_shouldSaveCatalogProviderWithoutCreator() {
        stubUser(UserRole.ADMIN);
        stubCategoryAndSave();

        ProviderResponse response = service.create(
                ALICE,
                new ProviderRequest(padded(CATALOG_PROVIDER_NAME), CATEGORY_ID, padded(LOGO_PATH)),
                ProviderType.CATALOG);

        SubscriptionProvider saved = savedProvider();
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.CATALOG);
        assertThat(saved.getCreatedBy()).isNull();
        assertThat(saved.getName()).isEqualTo(CATALOG_PROVIDER_NAME);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getLogoPath()).isEqualTo(LOGO_PATH);
        assertThat(response).isEqualTo(ProviderResponse.from(saved));
    }

    @Test
    void create_shouldSaveCustomProviderOwnedByTheUser() {
        User user = stubUser(UserRole.USER);
        stubCategoryAndSave();

        ProviderResponse response = service.create(
                ALICE, new ProviderRequest(padded(CUSTOM_PROVIDER_NAME), CATEGORY_ID, "   "), ProviderType.CUSTOM);

        SubscriptionProvider saved = savedProvider();
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.CUSTOM);
        assertThat(saved.getCreatedBy()).isSameAs(user);
        assertThat(saved.getName()).isEqualTo(CUSTOM_PROVIDER_NAME);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getLogoPath()).isNull();
        assertThat(response).isEqualTo(ProviderResponse.from(saved));
    }

    @Test
    void create_shouldSaveCustomProviderOwnedByTheAdmin() {
        User admin = stubUser(UserRole.ADMIN);
        stubCategoryAndSave();

        service.create(ALICE, new ProviderRequest(CUSTOM_PROVIDER_NAME, CATEGORY_ID, null), ProviderType.CUSTOM);

        SubscriptionProvider saved = savedProvider();
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.CUSTOM);
        assertThat(saved.getCreatedBy()).isSameAs(admin);
    }

    @Test
    void create_shouldRejectUnknownCategory() {
        stubUser(UserRole.USER);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());
        ProviderRequest request = new ProviderRequest(CUSTOM_PROVIDER_NAME, CATEGORY_ID, null);

        assertThatThrownBy(() -> service.create(ALICE, request, ProviderType.CUSTOM))
                .isInstanceOf(InvalidCategoryException.class)
                .hasMessageContaining(String.valueOf(CATEGORY_ID));
        verifyNoInteractions(subscriptionProviderRepository);
    }

    @Test
    void updateCustomProvider_shouldChangeNameCategoryAndLogo() {
        SubscriptionProvider provider = stubOwnCustomProvider();
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(otherCategory));

        ProviderResponse response = service.updateCustomProvider(ALICE, PROVIDER_ID, UPDATE_REQUEST);

        assertThat(provider.getName()).isEqualTo(NEW_NAME);
        assertThat(provider.getCategory()).isSameAs(otherCategory);
        assertThat(provider.getLogoPath()).isEqualTo(NEW_LOGO_PATH);
        assertThat(response).isEqualTo(ProviderResponse.from(provider));
    }

    @Test
    void updateCustomProvider_shouldRejectProviderTheUserDoesNotOwn() {
        when(subscriptionProviderRepository.findByIdAndCreatedByUsername(PROVIDER_ID, ALICE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCustomProvider(ALICE, PROVIDER_ID, UPDATE_REQUEST))
                .isInstanceOf(ProviderNotFoundException.class)
                .hasMessageContaining(String.valueOf(PROVIDER_ID));
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void updateCustomProvider_shouldRejectUnknownCategoryWithoutChangingTheProvider() {
        SubscriptionProvider provider = stubOwnCustomProvider();
        when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateCustomProvider(ALICE, PROVIDER_ID, UPDATE_REQUEST))
                .isInstanceOf(InvalidCategoryException.class);
        assertThat(provider.getName()).isEqualTo(CUSTOM_PROVIDER_NAME);
        assertThat(provider.getCategory()).isSameAs(category);
    }

    private void stubCategoryAndSave() {
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
        when(subscriptionProviderRepository.save(any(SubscriptionProvider.class)))
                .thenAnswer(returnsFirstArg());
    }

    private SubscriptionProvider savedProvider() {
        verify(subscriptionProviderRepository).save(providerCaptor.capture());
        return providerCaptor.getValue();
    }

    private SubscriptionProvider stubOwnCustomProvider() {
        SubscriptionProvider provider = TestProviders.custom(category, TestUsers.user(ALICE));
        when(subscriptionProviderRepository.findByIdAndCreatedByUsername(PROVIDER_ID, ALICE))
                .thenReturn(Optional.of(provider));
        return provider;
    }

    private User stubUser(UserRole role) {
        User user = TestUsers.user(ALICE, role);
        when(userRepository.getByUsername(ALICE)).thenReturn(user);
        return user;
    }

    private static String padded(String value) {
        return "  " + value + "  ";
    }
}
