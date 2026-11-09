package com.app.subly.service;

import static com.app.subly.testdata.TestUsers.ALICE;
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
import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.exception.user.UserDoesNotExistException;
import com.app.subly.exception.user.UserInfoUpdateException;
import com.app.subly.exception.user.UserIsConnectedException;
import com.app.subly.exception.user.WrongPasswordException;
import com.app.subly.model.user.User;
import com.app.subly.repository.UserRepository;
import com.app.subly.testdata.TestUsers;
import java.util.Optional;
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

    @Mock
    UserRepository repository;

    @Mock
    PasswordEncoder encoder;

    @InjectMocks
    UserService service;

    @Test
    void addNewUser_shouldRejectExistingUser() {
        when(encoder.encode(PASSWORD)).thenReturn(PASSWORD_HASH);
        when(repository.save(any(User.class))).thenThrow(new DataIntegrityViolationException("Some db error"));

        assertThatThrownBy(() -> service.addNewUser(ALICE, "Alice", PASSWORD, email(ALICE), null))
                .isInstanceOf(UserAlreadyExistsException.class);
        verify(repository).save(any(User.class));
    }

    @Test
    void getUserByUsername_shouldRejectUnknownUser() {
        when(repository.findByUsername(ALICE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getUserByUsername(ALICE)).isInstanceOf(UserDoesNotExistException.class);
    }

    @Test
    void deleteUserByUsername_shouldRejectWrongPassword() {
        stubExistingUser();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteUserByUsername(ALICE, PASSWORD))
                .isInstanceOf(WrongPasswordException.class);
    }

    @Test
    void deleteUserByUsername_shouldRejectUserReferencedByOtherRows() {
        stubExistingUser();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        doThrow(DataIntegrityViolationException.class).when(repository).deleteByUsername(ALICE);

        assertThatThrownBy(() -> service.deleteUserByUsername(ALICE, PASSWORD))
                .isInstanceOf(UserIsConnectedException.class);
    }

    @Test
    void deleteUserByUsername_shouldDeleteUserWithMatchingPassword() {
        stubExistingUser();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);

        service.deleteUserByUsername(ALICE, PASSWORD);

        verify(repository).deleteByUsername(ALICE);
    }

    @Test
    void updateUserPassword_shouldRejectWrongOldPassword() {
        User user = stubExistingUser();
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(false);

        assertThatThrownBy(() -> service.updateUserPassword(ALICE, PASSWORD, NEW_PASSWORD))
                .isInstanceOf(WrongPasswordException.class);
        assertThat(user.getPasswordHash()).isEqualTo(PASSWORD_HASH);
    }

    @Test
    void updateUserPassword_shouldStoreTheHashOfTheNewPassword() {
        User user = stubExistingUser();
        String newHash = "new_hash";
        when(encoder.matches(PASSWORD, PASSWORD_HASH)).thenReturn(true);
        when(encoder.encode(NEW_PASSWORD)).thenReturn(newHash);

        service.updateUserPassword(ALICE, PASSWORD, NEW_PASSWORD);

        assertThat(user.getPasswordHash()).isEqualTo(newHash);
    }

    @ParameterizedTest(name = "empty {0}")
    @MethodSource("emptyFieldUpdates")
    void updateUserInformation_shouldRejectEmptyValue(String field, UpdateUserInformationRequest request) {
        stubExistingUser();

        assertThatThrownBy(() -> service.updateUserInformation(ALICE, request))
                .isInstanceOf(UserInfoUpdateException.class)
                .hasMessageContaining("a non-empty " + field);
    }

    @Test
    void updateUserInformation_shouldChangeUsername() {
        User user = stubExistingUser();

        service.updateUserInformation(ALICE, usernameUpdate("alice_new"));

        verify(repository).flush();
        assertThat(user.getUsername()).isEqualTo("alice_new");
    }

    @Test
    void updateUserInformation_shouldChangeName() {
        User user = stubExistingUser();

        service.updateUserInformation(ALICE, nameUpdate("Alice Smith"));

        assertThat(user.getName()).isEqualTo("Alice Smith");
    }

    @Test
    void updateUserInformation_shouldChangeEmail() {
        User user = stubExistingUser();

        service.updateUserInformation(ALICE, emailUpdate(NEW_EMAIL));

        assertThat(user.getEmail()).isEqualTo(NEW_EMAIL);
    }

    @Test
    void updateUserInformation_shouldRejectExistingEmail() {
        stubExistingUser();
        doThrow(DataIntegrityViolationException.class).when(repository).flush();

        assertThatThrownBy(() -> service.updateUserInformation(ALICE, emailUpdate(email(BOB))))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void updateUserInformation_shouldRejectExistingUsername() {
        stubExistingUser();
        doThrow(new DataIntegrityViolationException("Duplicate username"))
                .when(repository)
                .flush();

        assertThatThrownBy(() -> service.updateUserInformation(ALICE, usernameUpdate(BOB)))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    static Stream<Arguments> emptyFieldUpdates() {
        return Stream.of(
                arguments("email", emailUpdate("")),
                arguments("username", usernameUpdate("")),
                arguments("name", nameUpdate("")));
    }

    private User stubExistingUser() {
        User user = TestUsers.user(ALICE);
        when(repository.findByUsername(ALICE)).thenReturn(Optional.of(user));
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
