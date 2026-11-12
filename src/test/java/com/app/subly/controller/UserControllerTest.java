package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.email;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.user.UpdateUserInformationRequest;
import com.app.subly.entity.user.User;
import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.exception.user.WrongPasswordException;
import com.app.subly.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserController.class)
@WithMockUser(username = ALICE)
class UserControllerTest extends AbstractWebMvcTest {

    private static final String DELETE_URL = "/users/delete/me";
    private static final String PASSWORD_URL = "/users/update/password/me";
    private static final String INFO_URL = "/users/update/info/me";
    private static final String PASSWORD = "largepassword123456";
    private static final String NEW_PASSWORD = "superlargepassword123456";

    @MockitoBean
    private UserService service;

    @Test
    void deleteUser_shouldReturn200WhenUserIsDeleted() throws Exception {
        MockHttpSession session = new MockHttpSession();
        sendJson(delete(DELETE_URL).session(session), passwordBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("The user account has been deleted"))
                .andExpect(jsonPath("$.error").doesNotExist())
                .andDo(result -> assertTrue(session.isInvalid()));

        verify(service).delete(ALICE, PASSWORD);
    }

    @Test
    void deleteUser_shouldReturn400WhenPasswordIsWrong() throws Exception {
        WrongPasswordException exception = new WrongPasswordException("The passwords don't match");
        doThrow(exception).when(service).delete(ALICE, PASSWORD);

        sendJson(delete(DELETE_URL), passwordBody())
                .andExpect(problem(HttpStatus.BAD_REQUEST, exception))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void updateCurrentUserPassword_shouldReturn400WhenPasswordsAreEqual() throws Exception {
        sendJson(put(PASSWORD_URL).with(csrf()), passwordChange(PASSWORD))
                .andExpect(validationError())
                .andExpect(jsonPath("$.errors[0].message").value("New password and old password must be different"));

        verifyNoInteractions(service);
    }

    @Test
    void updateCurrentUserPassword_shouldReturn200WhenPasswordIsChanged() throws Exception {
        MockHttpSession session = new MockHttpSession();
        sendJson(put(PASSWORD_URL).with(csrf()).session(session), passwordChange(NEW_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.message").value("The password has been changed successfully. Please log in again."))
                .andDo(result -> assertTrue(session.isInvalid()));

        verify(service).updatePassword(ALICE, PASSWORD, NEW_PASSWORD);
    }

    @Test
    void updateUserInformation_shouldReturn400WhenNoFieldIsProvided() throws Exception {
        sendJson(patch(INFO_URL).with(csrf()), "{}")
                .andExpect(validationError())
                .andExpect(jsonPath("$.errors[0].message").value("You must provide at least one property to change."));

        verifyNoInteractions(service);
    }

    @Test
    void updateUserInformation_shouldReturn400WhenUsernameIsTooLong() throws Exception {
        sendJson(patch(INFO_URL).with(csrf()), usernameChange("a".repeat(User.USERNAME_MAX_LENGTH + 1)))
                .andExpect(validationError())
                .andExpect(jsonPath("$.errors[0].field").value("newUsername"));

        verifyNoInteractions(service);
    }

    @Test
    void updateUserInformation_shouldAcceptUsernameOfMaxLength() throws Exception {
        MockHttpSession session = new MockHttpSession();
        sendJson(patch(INFO_URL).with(csrf()).session(session), usernameChange("a".repeat(User.USERNAME_MAX_LENGTH)))
                .andExpect(status().isOk())
                .andDo(result -> assertTrue(session.isInvalid()));

        verify(service).updateInformation(eq(ALICE), any(UpdateUserInformationRequest.class));
    }

    @Test
    void updateUserInformation_shouldReturn200WhenInformationIsUpdated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        sendJson(patch(INFO_URL).with(csrf()).session(session), emailChange("alice.new@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("The information has been saved. Please log in again to apply changes."))
                .andDo(result -> assertTrue(session.isInvalid()));

        verify(service).updateInformation(eq(ALICE), any(UpdateUserInformationRequest.class));
    }

    @Test
    void updateUserInformation_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("Email already exists");
        doThrow(exception).when(service).updateInformation(eq(ALICE), any(UpdateUserInformationRequest.class));

        sendJson(patch(INFO_URL).with(csrf()), emailChange(email(BOB)))
                .andExpect(problem(HttpStatus.CONFLICT, exception));
    }

    @Test
    @WithAnonymousUser
    void deleteUser_shouldReturn401ForAnonymousUser() throws Exception {
        sendJson(delete(DELETE_URL), passwordBody()).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void updateCurrentUserPassword_shouldReturn401ForAnonymousUser() throws Exception {
        sendJson(put(PASSWORD_URL).with(csrf()), passwordChange(NEW_PASSWORD)).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void updateUserInformation_shouldReturn401ForAnonymousUser() throws Exception {
        sendJson(patch(INFO_URL).with(csrf()), usernameChange("alice_new")).andExpect(status().isUnauthorized());
    }

    private static String passwordBody() {
        return """
                {"password": "%s"}
                """.formatted(PASSWORD);
    }

    private static String passwordChange(String newPassword) {
        return """
                {"oldPassword": "%s", "newPassword": "%s"}
                """.formatted(PASSWORD, newPassword);
    }

    private static String usernameChange(String username) {
        return """
                {"newUsername": "%s"}
                """.formatted(username);
    }

    private static String emailChange(String email) {
        return """
                {"newEmail": "%s"}
                """.formatted(email);
    }
}
