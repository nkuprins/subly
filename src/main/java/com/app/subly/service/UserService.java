package com.app.subly.service;

import com.app.subly.dto.user.UpdateUserInformationRequest;
import com.app.subly.entity.user.User;
import com.app.subly.enums.UserRole;
import com.app.subly.exception.user.*;
import com.app.subly.repository.UserRepository;
import java.time.ZoneId;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder encoder;

    private final InvitationService invitationService;
    private final SubscriptionService subscriptionService;
    private final ProviderService providerService;

    @Transactional
    public void create(String username, String name, String password, String email, ZoneId timeZone) {
        // Adds a new user into db from raw data. Throws UserAlreadyExistsException if
        // user exists with this username or email. It is supposed that the length of the name is validated
        // before calling this method.

        try {
            userRepository.saveAndFlush(
                    createUserFromRawData(username.strip(), name.strip(), password, email.strip(), timeZone));
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException("A user with the username or the email already exists");
        }
    }

    @Transactional
    public void delete(String username, String rawPassword) {
        // Make an attempt to delete a user with the given username
        // UserDoesNotExistException is raised if the user with the username was not found
        // UserIsConnectedException is raised in case some table's column refers to this user.
        // WrongPasswordException is raised if a raw password != password hash

        User user = getByUsername(username);

        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new WrongPasswordException("The passwords don't match");
        }

        try {
            cleanUserConnectedData(user.getId(), username);
            userRepository.deleteByUsername(username);
        } catch (DataIntegrityViolationException exception) {
            throw new UserIsConnectedException(
                    "Cannot delete the user because it is connected with some table's field");
        }
    }

    @Transactional
    public void updatePassword(String username, String rawOldPassword, String rawNewPassword) {
        User user = getByUsername(username);

        if (!encoder.matches(rawOldPassword, user.getPasswordHash())) {
            throw new WrongPasswordException("The passwords don't match");
        }

        user.setPasswordHash(encoder.encode(rawNewPassword));
    }

    @Transactional
    public void updateInformation(String oldUsername, UpdateUserInformationRequest newUserInfo) {
        User user = getByUsername(oldUsername);

        try {
            if (newUserInfo.isEmailProvided()) {
                updateField("email", user.getEmail(), newUserInfo.getNewEmail(), user::setEmail);
            }

            if (newUserInfo.isUsernameProvided()) {
                updateField("username", user.getUsername(), newUserInfo.getNewUsername(), user::setUsername);
            }

            if (newUserInfo.isNameProvided()) {
                updateField("name", user.getName(), newUserInfo.getNewName(), user::setName);
            }

            if (newUserInfo.isTimeZoneProvided()) {
                if (newUserInfo.getNewTimeZone() == null) {
                    throw new UserInfoUpdateException("You must provide a time zone");
                }
                user.setTimeZone(newUserInfo.getNewTimeZone());
            }

            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException("The user with provided email or username already exists");
        }
    }

    public User getByUsername(String username) {
        return userRepository
                .findByUsername(username)
                .orElseThrow(() -> new UserDoesNotExistException("No found user with the username"));
    }

    private User createUserFromRawData(String username, String name, String password, String email, ZoneId timeZone) {
        User user = new User(username, name, encoder.encode(password), email, UserRole.USER);
        if (timeZone != null) {
            user.setTimeZone(timeZone);
        }
        return user;
    }

    private void updateField(String label, String current, String provided, Consumer<String> setter) {
        String value = provided == null ? null : provided.strip();
        if (isNotValidForUpdating(current, value)) {
            throw new UserInfoUpdateException(
                    "You must provide a non-empty " + label + " that differs from your current");
        }
        setter.accept(value);
    }

    private boolean isNotValidForUpdating(String originalCredential, String providedCredential) {
        // Specially designed for update operations for email, username and name ONLY
        // 'originalCredential' - stored credential in the database
        // 'providedCredential' - credential provided in the request to update.
        // Checks whether 'originalCredential' can be updated by 'providedCredential'.
        // It can not be updated if:
        //   1) The provided credential is empty or null
        //   2) The provided credential is the same as the user's current credential

        if (providedCredential == null || providedCredential.isBlank()) {
            return true;
        }
        return originalCredential.equals(providedCredential);
    }

    private void cleanUserConnectedData(Long userId, String username) {
        // deletes all data related to the user
        invitationService.deleteInvitationFor(userId);
        subscriptionService.deleteAllSubscriptionsFor(userId);
        providerService.deleteAllProvidersCreatedBy(username);
    }
}
