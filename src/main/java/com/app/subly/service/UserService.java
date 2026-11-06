package com.app.subly.service;

import com.app.subly.dto.UpdateUserInformationRequest;
import com.app.subly.dto.UserResponse;
import com.app.subly.exception.*;
import com.app.subly.model.User;
import com.app.subly.model.UserRole;
import com.app.subly.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.List;


@Service
public class UserService {

    private final UserRepository repository;
    private final PasswordEncoder encoder;

    public UserService(UserRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    public void addNewUser(String username, String name, String password, String email, ZoneId timeZone) {
        // Adds a new user into db from raw data. Throws UserAlreadyExistsException if
        // user exists with this username or email. It is supposed that the length of the name is validated
        // before calling this method.

        try {
            this.repository.save(this.createUserFromRawData(username, name, password, email, timeZone));
        } catch (DataIntegrityViolationException exception) {
            throw new UserAlreadyExistsException("A user with the username or the email already exists");
        }
    }

    public UserResponse getUserById(Long id) {
        // Returns user with the corresponding id. If the user
        // doesn't exist, UserDoesNotExistException will be occurred
        // If an invalid id was passed, IllegalArgumentException will be raised

        if (id == null || id < 0) {
            throw new IllegalArgumentException("Id must be positive");
        }

        User user = this.repository.findById(id)
                .orElseThrow(() -> new UserDoesNotExistException("The user with the id doesn't exist"));

        return mapToDTO(user);
    }

    public List<UserResponse> listUsersByIds(List<Long> ids) {
        // Returns list of users with the corresponding ids.
        // If the length of the list with given ids differs from the found users,
        // IllegalArgumentException will be raised.

        if (ids == null) {
            throw new IllegalArgumentException("The list of ids is null");
        }

        List<User> result = this.repository.findAllById(ids);

        if (result.isEmpty() || result.size() != ids.size()) {
            throw new UserDoesNotExistException("No found all the users with those ids");
        }

        return result.stream().map(this::mapToDTO).toList();
    }


    @Transactional
    public void deleteUserByUsername(String username, String rawPassword) {
        // Make an attempt to delete a user with the given username
        // UserDoesNotExistException is raised if the user with the username was not found
        // UserIsConnectedError is raised in case some table's column refers to this user.
        // WrongPasswordException is raised if a raw password != password hash

        User user = this.getUserByUserName(username);

        if (!this.encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new WrongPasswordException("The passwords don't match");
        }

        try {
            this.repository.deleteByUsername(username);
        } catch (DataIntegrityViolationException exception) {
            throw new UserIsConnectedError("Cannot delete the user because it is connected with some table's field");
        }

    }


    @Transactional
    public void updateUserPassword(String username, String rawOldPassword, String rawNewPassword){
        User user = this.getUserByUserName(username);

        if(!this.encoder.matches(rawOldPassword, user.getPasswordHash())){
            throw new WrongPasswordException("The passwords don't match");
        }

        user.setPasswordHash(this.encoder.encode(rawNewPassword));
    }

    @Transactional
    public void updateUserInformation(String oldUsername, UpdateUserInformationRequest newUserInfo) {
        User user = this.getUserByUserName(oldUsername);

        try{
            if(newUserInfo.isEmailProvided()){
                if(this.isNotValidForUpdating(user.getEmail(), newUserInfo.getNewEmail())){
                    throw new UserInfoUpdateException("You must provide a non-empty email that differs from your current");
                }
                user.setEmail(newUserInfo.getNewEmail());
            }

            if(newUserInfo.isUsernameProvided()){
                if(this.isNotValidForUpdating(user.getUsername(), newUserInfo.getNewUsername())){
                    throw new UserInfoUpdateException("You must provide a non-empty username that differs from your current");
                }
                user.setUsername(newUserInfo.getNewUsername());
            }

            if(newUserInfo.isNameProvided()){
                if(this.isNotValidForUpdating(user.getName(), newUserInfo.getNewName())){
                    throw new UserInfoUpdateException("You must provide a non-empty name that differs from your current");
                }
                user.setName(newUserInfo.getNewName());
            }

            if(newUserInfo.isTimeZoneProvided()){
                if(newUserInfo.getNewTimeZone() == null){
                    throw new UserInfoUpdateException("You must provide a time zone");
                }
                user.setTimeZone(newUserInfo.getNewTimeZone());
            }

            this.repository.flush();
        } catch(DataIntegrityViolationException ex) {
            throw new UserAlreadyExistsException("The user with provided email or username already exists");
        }

    }

    public User getUserByUserName(String username){
        return this.repository.findByUsername(username).orElseThrow(() -> new UserDoesNotExistException(
                "No found user with the username"
        ));
    }

    private User createUserFromRawData(String username, String name, String password, String email,
                                       ZoneId timeZone) {
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setPasswordHash(encoder.encode(password));
        user.setEmail(email);
        user.setRole(UserRole.USER);
        if (timeZone != null) {
            user.setTimeZone(timeZone);
        }
        return user;
    }

    private boolean isNotValidForUpdating(String originalCredential, String providedCredential){
        // Specially designed for update operations for email, username and name ONLY
        // 'originalCredential' - stored credential in the database
        // 'providedCredential' - credential provided in the request to update.
        // Checks whether 'originalCredential' can be updated by 'providedCredential'.
        // It can not be updated if:
        //   1) The provided credential is empty or null
        //   2) The provided credential is the same as the user's current credential

        if(providedCredential == null || providedCredential.isBlank()){
            return true;
        }
        return originalCredential.equals(providedCredential);
    }

    private UserResponse mapToDTO(User user) {
        return new UserResponse(user.getUsername(), user.getEmail(), user.getRole());
    }
}
