package com.app.subly.service;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.ALICE_ID;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.PASSWORD_HASH;
import static com.app.subly.testdata.TestUsers.email;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.app.subly.dto.user.UpdateUserInformationRequest;
import com.app.subly.entity.user.User;
import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.exception.user.UserInfoUpdateException;
import com.app.subly.exception.user.UserIsConnectedException;
import com.app.subly.exception.user.WrongPasswordException;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestUsers;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final String PASSWORD = "secretPassword";
    private static final String NEW_PASSWORD = "newSecretPassword";
    private static final String NEW_EMAIL = "alice.new@example.com";
    private static final String NEW_USERNAME = "alice_new";
    private static final String NEW_NAME = "Alice Smith";

    @Mock
    UserRepository repository;

    @Mock
    PasswordEncoder encoder;

    @Mock
    InvitationService invitationService;

    @Mock
    SubscriptionService subscriptionService;

    @Mock
    ProviderService providerService;

    @InjectMocks
    UserService service;

    @Test
    void create_shouldRejectExistingUser() {
        when(encoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(repository.saveAndFlush(any(User.class))).thenThrow(DataIntegrityViolationException.class);
        String aliceEmail = email(ALICE);

        assertThatThrownBy(() -> service.create(ALICE, "Alice", PASSWORD, aliceEmail, null))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void delete_shouldRejectWrongPassword() {
        stubAliceExists();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(ALICE, PASSWORD)).isInstanceOf(WrongPasswordException.class);
    }

    @Test
    void delete_shouldRejectUserReferencedByOtherRows() {
        stubAliceExists();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        doThrow(DataIntegrityViolationException.class).when(repository).deleteByUsername(ALICE);

        assertThatThrownBy(() -> service.delete(ALICE, PASSWORD)).isInstanceOf(UserIsConnectedException.class);
    }

    @Test
    void delete_shouldDeleteUserWithMatchingPassword() {
        stubAliceExists();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        service.delete(ALICE, PASSWORD);

        verify(repository).deleteByUsername(ALICE);
    }

    @Test
    void updatePassword_shouldRejectWrongOldPassword() {
        User user = stubAliceExists();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.updatePassword(ALICE, PASSWORD, NEW_PASSWORD))
                .isInstanceOf(WrongPasswordException.class);
        assertThat(user.getPasswordHash()).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void updatePassword_shouldStoreTheHashOfTheNewPassword() {
        User user = stubAliceExists();
        String newHash = "new_hash";
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(encoder.encode(NEW_PASSWORD)).thenReturn(newHash);

        service.updatePassword(ALICE, PASSWORD, NEW_PASSWORD);

        assertThat(user.getPasswordHash()).isEqualTo(newHash);
    }

    @ParameterizedTest(name = "empty {0}")
    @MethodSource("emptyFieldUpdates")
    void updateInformation_shouldRejectEmptyValue(String field, UpdateUserInformationRequest request) {
        stubAliceExists();

        assertThatThrownBy(() -> service.updateInformation(ALICE, request))
                .isInstanceOf(UserInfoUpdateException.class)
                .hasMessageContaining("a non-empty " + field);
    }

    @Test
    void updateInformation_shouldChangeUsername() {
        User user = stubAliceExists();

        service.updateInformation(ALICE, usernameUpdate(NEW_USERNAME));

        verify(repository).flush();
        assertThat(user.getUsername()).isEqualTo(NEW_USERNAME);
    }

    @Test
    void updateInformation_shouldChangeName() {
        User user = stubAliceExists();

        service.updateInformation(ALICE, nameUpdate(NEW_NAME));

        assertThat(user.getName()).isEqualTo(NEW_NAME);
    }

    @Test
    void updateInformation_shouldChangeEmail() {
        User user = stubAliceExists();

        service.updateInformation(ALICE, emailUpdate(NEW_EMAIL));

        assertThat(user.getEmail()).isEqualTo(NEW_EMAIL);
    }

    @Test
    void updateInformation_shouldRejectExistingEmail() {
        stubAliceExists();
        doThrow(DataIntegrityViolationException.class).when(repository).flush();
        UpdateUserInformationRequest request = emailUpdate(email(BOB));

        assertThatThrownBy(() -> service.updateInformation(ALICE, request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    static Stream<Arguments> emptyFieldUpdates() {
        return Stream.of(
                arguments("email", emailUpdate("")),
                arguments("username", usernameUpdate("")),
                arguments("name", nameUpdate("")));
    }

    private User stubAliceExists() {
        User user = TestUsers.userWithId(ALICE, ALICE_ID);
        when(repository.getUser(ALICE)).thenReturn(user);
        return user;
    }

    private static UpdateUserInformationRequest emailUpdate(String email) {
        UpdateUserInformationRequest request = new UpdateUserInformationRequest();
        request.setNewEmail(email);
        return request;
    }

    private static UpdateUserInformationRequest usernameUpdate(String username) {
        UpdateUserInformationRequest request = new UpdateUserInformationRequest();
        request.setNewUsername(username);
        return request;
    }

    private static UpdateUserInformationRequest nameUpdate(String name) {
        UpdateUserInformationRequest request = new UpdateUserInformationRequest();
        request.setNewName(name);
        return request;
    }
}
