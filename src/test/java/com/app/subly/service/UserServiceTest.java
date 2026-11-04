package com.app.subly.service;

import com.app.subly.dto.UpdateUserInformationRequest;
import com.app.subly.exceptions.*;
import com.app.subly.models.User;
import com.app.subly.repository.UserRepository;
import com.app.subly.services.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private PasswordEncoder encoder;

    @InjectMocks
    private UserService service;


    // method_shouldDoXWhenY
    // get_ShouldReturnScore10, multiplyReserveShouldCallExternalSystemCorrectly

    @Test
    public void addUserShouldThrowExceptionIfUserExists() {
        when(this.encoder.encode("123"))
                .thenReturn("123");

        doThrow(new DataIntegrityViolationException("Some db error"))
                .when(this.repository).save(any(User.class));

        assertThrows(UserAlreadyExistsException.class,
                () -> this.service.addNewUser("andris123", "Andris", "123", "andris@gmail.com"));
        verify(repository).save(any(User.class));
    }

    @Test
    public void getUserShouldThrowExceptionIfUserNotExist() {
        when(this.repository.findByUsername("andris123"))
                .thenReturn(Optional.empty());

        assertThrows(UserDoesNotExistException.class,
                () -> this.service.getUserByUserName("andris123"));
    }

    @Test
    public void deleteUserShouldThrowExceptionIfPasswordsUnmatched() {
        User user = new User();
        user.setUsername("andris123");
        user.setPasswordHash("some_hash");

        when(this.repository.findByUsername("andris123"))
                .thenReturn(Optional.of(user));

        when(this.encoder.matches("secretPassword", "some_hash"))
                .thenReturn(false);

        assertThrows(WrongPasswordException.class, ()
                -> this.service.deleteUserByUsername("andris123", "secretPassword"));
    }

    @Test
    public void deleteUserShouldThrowExceptionIfUserConnectedWithTable() {
        User user = new User();
        user.setUsername("andris123");
        user.setPasswordHash("some_hash");

        when(this.repository.findByUsername("andris123"))
                .thenReturn(Optional.of(user));

        when(this.encoder.matches("secretPassword", "some_hash"))
                .thenReturn(true);

        doThrow(DataIntegrityViolationException.class)
                .when(this.repository).deleteByUsername("andris123");

        assertThrows(UserIsConnectedError.class, ()
                -> this.service.deleteUserByUsername("andris123", "secretPassword"));
    }

    @Test
    public void deleteUserShouldBeCompletedCorrectly() {
        User user = new User();
        user.setUsername("andris123");
        user.setPasswordHash("some_hash");

        when(this.repository.findByUsername("andris123"))
                .thenReturn(Optional.of(user));

        when(this.encoder.matches("secretPassword", "some_hash"))
                .thenReturn(true);

        assertDoesNotThrow(() -> this.service.deleteUserByUsername("andris123", "secretPassword"));
        verify(encoder).matches("secretPassword", "some_hash");
        verify(repository).deleteByUsername("andris123");
    }


    @Test
    public void updatePasswordShouldFailIfPasswordsUnmatched() {
        User user = new User();
        user.setUsername("andris123");
        user.setPasswordHash("some_hash");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        when(this.encoder.matches("realPassword", "some_hash")).thenReturn(false);

        assertThrows(WrongPasswordException.class, ()
                -> this.service.updateUserPassword("andris123", "realPassword", "newRealPassword"));

        assertThat(user.getPasswordHash()).isEqualTo("some_hash");
    }

    @Test
    public void updatePasswordShouldBeCompletedCorrectly() {
        User user = new User();
        user.setUsername("andris123");
        user.setPasswordHash("some_hash");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        when(this.encoder.matches("realPassword", "some_hash")).thenReturn(true);
        when(this.encoder.encode("newRealPassword")).thenReturn("new_some_hash");

        assertDoesNotThrow(() -> this.service.updateUserPassword(
                "andris123", "realPassword", "newRealPassword"));

        assertThat(user.getPasswordHash()).isEqualTo("new_some_hash");
    }

    @Test
    public void updateInformationShouldFailWithEmptyEmail() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        UpdateUserInformationRequest fake_request = new UpdateUserInformationRequest();
        fake_request.setNewEmail("");

        UserInfoUpdateException ex = assertThrows(UserInfoUpdateException.class,
                () -> this.service.updateUserInformation("andris123", fake_request));

        assertThat(ex.getMessage()).containsIgnoringCase("a non-empty email");
    }

    @Test
    public void updateInformationShouldFailWithEmptyUsername() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        UpdateUserInformationRequest fake_request = new UpdateUserInformationRequest();
        fake_request.setNewUsername("");

        UserInfoUpdateException ex = assertThrows(UserInfoUpdateException.class,
                () -> this.service.updateUserInformation("andris123", fake_request));

        assertThat(ex.getMessage()).containsIgnoringCase("a non-empty username");
    }

    @Test
    public void updateInformationShouldFailWithEmptyName() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        UpdateUserInformationRequest fake_request = new UpdateUserInformationRequest();
        fake_request.setNewName("");

        UserInfoUpdateException ex = assertThrows(UserInfoUpdateException.class,
                () -> this.service.updateUserInformation("andris123", fake_request));

        assertThat(ex.getMessage()).containsIgnoringCase("a non-empty name");
    }

    @Test
    public void updateInformationShouldChangeUsernameCorrectly() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        UpdateUserInformationRequest fake_request = new UpdateUserInformationRequest();
        fake_request.setNewUsername("andris_new");

        assertDoesNotThrow(() -> this.service.updateUserInformation("andris123", fake_request));
        verify(repository).flush();
        assertThat(user.getUsername()).isEqualTo("andris_new");
    }

    @Test
    public void updateInformationShouldChangeNameCorrectly() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        UpdateUserInformationRequest fake_request = new UpdateUserInformationRequest();
        fake_request.setNewName("Alexandr");

        assertDoesNotThrow(() -> this.service.updateUserInformation("andris123", fake_request));

        assertThat(user.getName()).isEqualTo("Alexandr");
    }

    @Test
    public void updateInformationShouldChangeEmailCorrectly() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        UpdateUserInformationRequest fake_request = new UpdateUserInformationRequest();
        fake_request.setNewEmail("my_new_email@gmail.com");

        assertDoesNotThrow(() -> this.service.updateUserInformation("andris123", fake_request));

        assertThat(user.getEmail()).isEqualTo("my_new_email@gmail.com");
    }

    @Test
    public void updateInformationShouldFailIfEmailExists() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(this.repository.findByUsername("andris123")).thenReturn(Optional.of(user));

        UpdateUserInformationRequest fake_request = new UpdateUserInformationRequest();
        fake_request.setNewEmail("my_new_email@gmail.com");

        doThrow(DataIntegrityViolationException.class).when(this.repository).flush();

        assertThrows(UserAlreadyExistsException.class,
                () -> this.service.updateUserInformation("andris123", fake_request));

    }

    @Test
    public void updateInformationShouldFailIfUsernameExists() {
        User user = new User();
        user.setEmail("andris@gmail.com");
        user.setName("Andris");
        user.setUsername("andris123");

        when(repository.findByUsername("andris123"))
                .thenReturn(Optional.of(user));

        UpdateUserInformationRequest request =
                new UpdateUserInformationRequest();

        request.setNewUsername("existingUsername");

        doThrow(new DataIntegrityViolationException("Duplicate username"))
                .when(repository).flush();

        assertThrows(
                UserAlreadyExistsException.class,
                () -> service.updateUserInformation("andris123", request)
        );
    }
}
