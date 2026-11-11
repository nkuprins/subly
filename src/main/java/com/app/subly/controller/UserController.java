package com.app.subly.controller;

import com.app.subly.dto.*;
import com.app.subly.dto.user.ChangePasswordRequest;
import com.app.subly.dto.user.DeleteCurrentUserRequest;
import com.app.subly.dto.user.UpdateUserInformationRequest;
import com.app.subly.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService service;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    @DeleteMapping("/delete/me")
    public OperationResponse deleteUser(
            @AuthenticationPrincipal UserDetails details,
            @RequestBody @Valid DeleteCurrentUserRequest deleteRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        service.deleteUserByUsername(details.getUsername(), deleteRequest.password());
        logoutCurrentSession(request, response);
        return new OperationResponse("The user account has been deleted");
    }

    @PutMapping("/update/password/me")
    public OperationResponse updateCurrentUserPassword(
            @AuthenticationPrincipal UserDetails details,
            @Valid @RequestBody ChangePasswordRequest changeRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        service.updateUserPassword(details.getUsername(), changeRequest.oldPassword(), changeRequest.newPassword());
        logoutCurrentSession(request, response);
        return new OperationResponse("The password has been changed successfully. Please log in again.");
    }

    @PatchMapping("/update/info/me")
    public OperationResponse updateUserInformation(
            @AuthenticationPrincipal UserDetails details,
            @Valid @RequestBody UpdateUserInformationRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        service.updateUserInformation(details.getUsername(), updateRequest);
        logoutCurrentSession(request, response);
        return new OperationResponse("The information has been saved. Please log in again to apply changes.");
    }

    private void logoutCurrentSession(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logoutHandler.logout(request, response, authentication);
    }
}
