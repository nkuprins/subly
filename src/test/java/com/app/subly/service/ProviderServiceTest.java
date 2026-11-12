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

import com.app.subly.dto.form.CreateProviderForm;
import com.app.subly.entity.category.Category;
import com.app.subly.entity.provider.SubscriptionProvider;
import com.app.subly.entity.user.User;
import com.app.subly.enums.ProviderType;
import com.app.subly.enums.UserRole;
import com.app.subly.exception.category.CategoryNotFoundException;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestProviders;
import com.app.subly.testdata.TestUsers;
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
    static final String LOGO_PATH = "/logo.svg";

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

    @Test
    void create_shouldSaveCatalogProviderWithoutCreator() {
        stubCategoryAndSave();

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

    @Test
    void create_shouldSaveCustomProviderOwnedByTheUser() {
        User user = stubUser(UserRole.USER);
        stubCategoryAndSave();

        service.create(
                ALICE, new CreateProviderForm(padded(CUSTOM_PROVIDER_NAME), CATEGORY_ID, "   "), ProviderType.CUSTOM);

        SubscriptionProvider saved = savedProvider();
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.CUSTOM);
        assertThat(saved.getCreatedBy()).isSameAs(user);
        assertThat(saved.getName()).isEqualTo(CUSTOM_PROVIDER_NAME);
        assertThat(saved.getCategory()).isSameAs(category);
        assertThat(saved.getLogoPath()).isNull();
    }

    @Test
    void create_shouldSaveCustomProviderOwnedByTheAdmin() {
        User admin = stubUser(UserRole.ADMIN);
        stubCategoryAndSave();

        service.create(ALICE, new CreateProviderForm(CUSTOM_PROVIDER_NAME, CATEGORY_ID, null), ProviderType.CUSTOM);

        SubscriptionProvider saved = savedProvider();
        assertThat(saved.getProviderType()).isEqualTo(ProviderType.CUSTOM);
        assertThat(saved.getCreatedBy()).isSameAs(admin);
    }

    @Test
    void create_shouldRejectUnknownCategory() {
        when(categoryRepository.getCategory(CATEGORY_ID)).thenThrow(new CategoryNotFoundException(CATEGORY_ID));
        CreateProviderForm form = new CreateProviderForm(CUSTOM_PROVIDER_NAME, CATEGORY_ID, null);

        assertThatThrownBy(() -> service.create(ALICE, form, ProviderType.CUSTOM))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessageContaining(String.valueOf(CATEGORY_ID));
        verifyNoInteractions(subscriptionProviderRepository);
    }

    private void stubCategoryAndSave() {
        when(categoryRepository.getCategory(CATEGORY_ID)).thenReturn(category);
        when(subscriptionProviderRepository.save(any(SubscriptionProvider.class)))
                .thenAnswer(returnsFirstArg());
    }

    private SubscriptionProvider savedProvider() {
        verify(subscriptionProviderRepository).save(providerCaptor.capture());
        return providerCaptor.getValue();
    }

    private User stubUser(UserRole role) {
        User user = TestUsers.user(ALICE, role);
        when(userRepository.getUser(ALICE)).thenReturn(user);
        return user;
    }

    private static String padded(String value) {
        return "  " + value + "  ";
    }
}
