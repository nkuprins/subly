package com.app.subly.controller;

import com.app.subly.dto.*;
import com.app.subly.exceptions.*;
import com.app.subly.services.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping("/register")
    public ResponseEntity<OperationResponse> createNewUser(@Valid @RequestBody CreateUserRequest request) {
        try {
            this.service.addNewUser(request.getUsername(), request.getName(), request.getPassword(), request.getEmail());
            return ResponseEntity.ok(new OperationResponse(false, "The user has been registered successfully"));
        } catch (UserAlreadyExistsException exception) {
            return ResponseEntity.badRequest().body(new OperationResponse(true, exception.getMessage()));
        }
    }

    @DeleteMapping("/delete/me")
    public ResponseEntity<OperationResponse> deleteUser(@AuthenticationPrincipal UserDetails details,
                                                        @RequestBody @Valid DeleteCurrentUserRequest deleteRequest,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        try {
            this.service.deleteUserByUsername(details.getUsername(), deleteRequest.getPassword());
            this.logoutCurrentSession(request, response);
            return ResponseEntity.ok(new OperationResponse(false, "The user account has been deleted"));
        } catch (UserDoesNotExistException | UserIsConnectedError | WrongPasswordException ex) {
            return ResponseEntity.badRequest().body(new OperationResponse(true, ex.getMessage()));
        }
    }

    @PutMapping("/update/password/me")
    public ResponseEntity<OperationResponse> updateCurrentUserPassword(@AuthenticationPrincipal UserDetails details,
                                                                       @Valid @RequestBody ChangePasswordRequest changeRequest,
                                                                       HttpServletRequest request,
                                                                       HttpServletResponse response) {

        if(changeRequest.getOldPassword().equals(changeRequest.getNewPassword())){
            return ResponseEntity.badRequest().body(new OperationResponse(true, "New password and old password must be different"));
        }

        try {
            this.service.updateUserPassword(
                    details.getUsername(),
                    changeRequest.getOldPassword(),
                    changeRequest.getNewPassword());
            this.logoutCurrentSession(request, response);
            return ResponseEntity.ok(new OperationResponse(false, "The password has been changed successfully. Please log in again."));
        } catch (UserDoesNotExistException | WrongPasswordException ex) {
            return ResponseEntity.badRequest().body(new OperationResponse(true, ex.getMessage()));
        }

    }

    @PatchMapping("/update/info/me")
    public ResponseEntity<OperationResponse> updateUserInformation(@AuthenticationPrincipal UserDetails details,
                                                                   @Valid @RequestBody UpdateUserInformationRequest updateRequest,
                                                                   HttpServletRequest request,
                                                                   HttpServletResponse response) {

        if (!updateRequest.isUsernameProvided() && !updateRequest.isEmailProvided() && !updateRequest.isNameProvided()) {
            return ResponseEntity.badRequest().body(
                    new OperationResponse(true, "You must provide at least one property to change."));
        }

        try {
            this.service.updateUserInformation(details.getUsername(), updateRequest);
            this.logoutCurrentSession(request, response);
            return ResponseEntity.ok(new OperationResponse(false, "The information has been saved. Please log in again to apply changes."));
        } catch (UserDoesNotExistException | UserInfoUpdateException | UserAlreadyExistsException ex) {
            return ResponseEntity.badRequest().body(new OperationResponse(true, ex.getMessage()));
        }
    }

    @PostMapping("/logout/me")
    public ResponseEntity<OperationResponse> logoutCurrentUser(HttpServletRequest request,
                                                               HttpServletResponse response) {
        this.logoutCurrentSession(request, response);
        return ResponseEntity.noContent().build();
    }

    private void logoutCurrentSession(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logoutHandler.logout(request, response, authentication);
    }
}
