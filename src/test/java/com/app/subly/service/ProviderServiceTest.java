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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

    @Mock
    SubscriptionProviderRepository subscriptionProviderRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProviderService providerService;

    @Captor
    ArgumentCaptor<SubscriptionProvider> providerCaptor;

    User owner;
    Category category;

    @BeforeEach
    void setUp() {
        owner = TestUsers.user(ALICE);
        category = TestProviders.category();
    }

    @Nested
    class Create {

        @BeforeEach
        void categoryAndSaveWork() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(subscriptionProviderRepository.save(any(SubscriptionProvider.class)))
                    .thenAnswer(returnsFirstArg());
        }

        @Test
        void create_shouldSaveAdminProviderAsCatalogProviderWithoutCreator() {
            User admin = stubUser(ALICE, UserRole.ADMIN);

            ProviderResponse response = providerService.create(
                    admin.getUsername(),
                    new ProviderRequest("  Test catalog provider  ", CATEGORY_ID, "  /logo.svg  "));

            SubscriptionProvider saved = savedProvider();
            assertThat(saved.getProviderType()).isEqualTo(ProviderType.CATALOG);
            assertThat(saved.getCreatedBy()).isNull();
            assertThat(saved.getName()).isEqualTo(CATALOG_PROVIDER_NAME);
            assertThat(saved.getCategory()).isSameAs(category);
            assertThat(saved.getLogoPath()).isEqualTo("/logo.svg");
            assertThat(response).isEqualTo(ProviderResponse.from(saved));
        }

        @Test
        void create_shouldSaveUserProviderAsCustomProviderOwnedByTheUser() {
            User user = stubUser(ALICE, UserRole.USER);

            ProviderResponse response =
                    providerService.create(ALICE, new ProviderRequest("  My category  ", CATEGORY_ID, "   "));

            SubscriptionProvider saved = savedProvider();
            assertThat(saved.getProviderType()).isEqualTo(ProviderType.CUSTOM);
            assertThat(saved.getCreatedBy()).isSameAs(user);
            assertThat(saved.getName()).isEqualTo("My category");
            assertThat(saved.getLogoPath()).isNull();
            assertThat(response).isEqualTo(ProviderResponse.from(saved));
        }

        private SubscriptionProvider savedProvider() {
            verify(subscriptionProviderRepository).save(providerCaptor.capture());
            return providerCaptor.getValue();
        }
    }

    @Test
    void create_shouldRejectUnknownCategory() {
        stubUser(ALICE, UserRole.USER);
        when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                        providerService.create(ALICE, new ProviderRequest(CUSTOM_PROVIDER_NAME, CATEGORY_ID, null)))
                .isInstanceOf(InvalidCategoryException.class)
                .hasMessageContaining(String.valueOf(CATEGORY_ID));
        verifyNoInteractions(subscriptionProviderRepository);
    }

    @Nested
    class UpdateCustom {

        final Category otherCategory = new Category("Test otherCategory");
        final ProviderRequest request = new ProviderRequest("  Renamed  ", OTHER_CATEGORY_ID, "/new.svg");

        @Test
        void updateCustomProvider_shouldChangeNameCategoryAndLogo() {
            SubscriptionProvider provider = stubOwnCustomProvider();
            when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.of(otherCategory));

            ProviderResponse response = providerService.updateCustomProvider(ALICE, PROVIDER_ID, request);

            assertThat(provider.getName()).isEqualTo("Renamed");
            assertThat(provider.getCategory()).isSameAs(otherCategory);
            assertThat(provider.getLogoPath()).isEqualTo("/new.svg");
            assertThat(response).isEqualTo(ProviderResponse.from(provider));
        }

        @Test
        void updateCustomProvider_shouldRejectProviderTheUserDoesNotOwn() {
            when(subscriptionProviderRepository.findByIdAndCreatedByUsername(PROVIDER_ID, ALICE))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.updateCustomProvider(ALICE, PROVIDER_ID, request))
                    .isInstanceOf(ProviderNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PROVIDER_ID));
            verifyNoInteractions(categoryRepository);
        }

        @Test
        void updateCustomProvider_shouldRejectUnknownCategoryWithoutChangingTheProvider() {
            SubscriptionProvider provider = stubOwnCustomProvider();
            when(categoryRepository.findById(OTHER_CATEGORY_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.updateCustomProvider(ALICE, PROVIDER_ID, request))
                    .isInstanceOf(InvalidCategoryException.class);
            assertThat(provider.getName()).isEqualTo(CUSTOM_PROVIDER_NAME);
            assertThat(provider.getCategory()).isSameAs(category);
        }

        private SubscriptionProvider stubOwnCustomProvider() {
            SubscriptionProvider provider = TestProviders.custom(category, owner);
            when(subscriptionProviderRepository.findByIdAndCreatedByUsername(PROVIDER_ID, ALICE))
                    .thenReturn(Optional.of(provider));
            return provider;
        }
    }

    private User stubUser(String username, UserRole role) {
        User user = TestUsers.user(username, role);
        when(userRepository.getByUsername(username)).thenReturn(user);
        return user;
    }
}
