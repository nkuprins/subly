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
        // UserIsConnectedException is raised in case some table's column refers to this user.
        // WrongPasswordException is raised if a raw password != password hash

        User user = getUser(username);

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
        User user = getUser(username);

        if (!encoder.matches(rawOldPassword, user.getPasswordHash())) {
            throw new WrongPasswordException("The passwords don't match");
        }

        user.setPasswordHash(encoder.encode(rawNewPassword));
    }

    @Transactional
    public void updateInformation(String oldUsername, UpdateUserInformationRequest request) {
        User user = getUser(oldUsername);

        try {
            if (request.isEmailProvided()) {
                updateField("email", user.getEmail(), request.getNewEmail(), user::setEmail);
            }

            if (request.isUsernameProvided()) {
                updateField("username", user.getUsername(), request.getNewUsername(), user::setUsername);
            }

            if (request.isNameProvided()) {
                updateField("name", user.getName(), request.getNewName(), user::setName);
            }

            if (request.isTimeZoneProvided()) {
                if (request.getNewTimeZone() == null) {
                    throw new UserInfoUpdateException("You must provide a time zone");
                }
                user.setTimeZone(request.getNewTimeZone());
            }

            userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException("The user with provided email or username already exists");
        }
    }

    public User getUser(String username) {
        return userRepository.getUser(username);
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
        if (!isValidReplacement(current, value)) {
            throw new UserInfoUpdateException(
                    "You must provide a non-empty " + label + " that differs from your current");
        }
        setter.accept(value);
    }

    private boolean isValidReplacement(String current, String provided) {
        return provided != null && !provided.isBlank() && !provided.equals(current);
    }

    private void cleanUserConnectedData(long userId, String username) {
        // deletes all data related to the user
        invitationService.deleteAllInvitationsFor(userId);
        subscriptionService.deleteAllSubscriptionsFor(userId);
        providerService.deleteAllProvidersCreatedBy(username);
    }
}
