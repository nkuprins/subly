package com.app.subly.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.app.subly.dto.provider.ProviderRequest;
import com.app.subly.dto.provider.ProviderResponse;
import com.app.subly.exception.category.InvalidCategoryException;
import com.app.subly.exception.subscription.ProviderNotFoundException;
import com.app.subly.model.TestUsers;
import com.app.subly.model.User;
import com.app.subly.model.UserRole;
import com.app.subly.model.subscription.Category;
import com.app.subly.model.subscription.ProviderType;
import com.app.subly.model.subscription.SubscriptionProvider;
import com.app.subly.repository.CategoryRepository;
import com.app.subly.repository.SubscriptionProviderRepository;
import com.app.subly.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

    static final String OWNER = "test-owner";
    static final long GYM_ID = 4L;
    static final long MUSIC_ID = 5L;
    static final long PROVIDER_ID = 3L;

    @Mock
    SubscriptionProviderRepository subscriptionProviderRepository;

    @Mock
    CategoryRepository categoryRepository;

    @Mock
    UserRepository userRepository;

    @Captor
    ArgumentCaptor<SubscriptionProvider> providerCaptor;

    ProviderService providerService;
    User owner;
    Category gym;

    @BeforeEach
    void setUp() {
        providerService = new ProviderService(subscriptionProviderRepository, categoryRepository, userRepository);
        owner = TestUsers.user(OWNER);
        gym = new Category("Test gym");
    }

    @Nested
    class Create {

        @BeforeEach
        void categoryAndSaveWork() {
            given(categoryRepository.findById(GYM_ID)).willReturn(Optional.of(gym));
            given(subscriptionProviderRepository.save(any(SubscriptionProvider.class)))
                    .willAnswer(returnsFirstArg());
        }

        @Test
        void adminCreatesCatalogProviderWithoutCreator() {
            User admin = givenUser("test-admin", UserRole.ADMIN);

            ProviderResponse response = providerService.create(
                    admin.getUsername(), new ProviderRequest("  Netflix  ", GYM_ID, "  /logo.svg  "));

            SubscriptionProvider saved = savedProvider();
            assertThat(saved.getProviderType()).isEqualTo(ProviderType.CATALOG);
            assertThat(saved.getCreatedBy()).isNull();
            assertThat(saved.getName()).isEqualTo("Netflix");
            assertThat(saved.getCategory()).isSameAs(gym);
            assertThat(saved.getLogoPath()).isEqualTo("/logo.svg");
            assertThat(response).isEqualTo(ProviderResponse.from(saved));
        }

        @Test
        void userCreatesCustomProviderOwnedByThemselves() {
            User user = givenUser(OWNER, UserRole.USER);

            ProviderResponse response = providerService.create(OWNER, new ProviderRequest("  My gym  ", GYM_ID, "   "));

            SubscriptionProvider saved = savedProvider();
            assertThat(saved.getProviderType()).isEqualTo(ProviderType.CUSTOM);
            assertThat(saved.getCreatedBy()).isSameAs(user);
            assertThat(saved.getName()).isEqualTo("My gym");
            assertThat(saved.getLogoPath()).isNull();
            assertThat(response).isEqualTo(ProviderResponse.from(saved));
        }

        private SubscriptionProvider savedProvider() {
            then(subscriptionProviderRepository).should().save(providerCaptor.capture());
            return providerCaptor.getValue();
        }
    }

    @Test
    void createRejectsUnknownCategory() {
        givenUser(OWNER, UserRole.USER);
        given(categoryRepository.findById(GYM_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> providerService.create(OWNER, new ProviderRequest("My gym", GYM_ID, null)))
                .isInstanceOf(InvalidCategoryException.class)
                .hasMessageContaining(String.valueOf(GYM_ID));
        then(subscriptionProviderRepository).shouldHaveNoInteractions();
    }

    private User givenUser(String username, UserRole role) {
        User user = TestUsers.user(username, role);
        given(userRepository.getByUsername(username)).willReturn(user);
        return user;
    }

    @Nested
    class UpdateCustom {

        final Category music = new Category("Test music");
        final ProviderRequest request = new ProviderRequest("  Renamed  ", MUSIC_ID, "/new.svg");

        @Test
        void changesNameCategoryAndLogo() {
            SubscriptionProvider provider = SubscriptionProvider.custom("My gym", gym, owner);
            given(subscriptionProviderRepository.findByIdAndCreatedByUsername(PROVIDER_ID, OWNER))
                    .willReturn(Optional.of(provider));
            given(categoryRepository.findById(MUSIC_ID)).willReturn(Optional.of(music));

            ProviderResponse response = providerService.updateCustomProvider(OWNER, PROVIDER_ID, request);

            assertThat(provider.getName()).isEqualTo("Renamed");
            assertThat(provider.getCategory()).isSameAs(music);
            assertThat(provider.getLogoPath()).isEqualTo("/new.svg");
            assertThat(response).isEqualTo(ProviderResponse.from(provider));
        }

        @Test
        void rejectsProviderTheUserDoesNotOwn() {
            given(subscriptionProviderRepository.findByIdAndCreatedByUsername(PROVIDER_ID, OWNER))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.updateCustomProvider(OWNER, PROVIDER_ID, request))
                    .isInstanceOf(ProviderNotFoundException.class)
                    .hasMessageContaining(String.valueOf(PROVIDER_ID));
            then(categoryRepository).shouldHaveNoInteractions();
        }

        @Test
        void rejectsUnknownCategoryWithoutChangingTheProvider() {
            SubscriptionProvider provider = SubscriptionProvider.custom("My gym", gym, owner);
            given(subscriptionProviderRepository.findByIdAndCreatedByUsername(PROVIDER_ID, OWNER))
                    .willReturn(Optional.of(provider));
            given(categoryRepository.findById(MUSIC_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> providerService.updateCustomProvider(OWNER, PROVIDER_ID, request))
                    .isInstanceOf(InvalidCategoryException.class);
            assertThat(provider.getName()).isEqualTo("My gym");
            assertThat(provider.getCategory()).isSameAs(gym);
        }
    }
}
