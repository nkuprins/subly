package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static com.app.subly.testdata.TestUsers.BOB;
import static com.app.subly.testdata.TestUsers.email;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.dto.user.UpdateUserInformationRequest;
import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.exception.user.WrongPasswordException;
import com.app.subly.model.user.User;
import com.app.subly.service.UserService;
import com.app.subly.testutil.JsonBody;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(UserController.class)
@WithMockUser(username = ALICE)
class UserControllerTest extends AbstractWebMvcTest {

    static final String DELETE_URL = "/users/delete/me";
    static final String PASSWORD_URL = "/users/update/password/me";
    static final String INFO_URL = "/users/update/info/me";
    static final String LOGOUT_URL = "/users/logout/me";
    static final String PASSWORD = "largepassword123456";
    static final String NEW_PASSWORD = "superlargepassword123456";

    @MockitoBean
    UserService service;

    @Test
    void deleteUser_shouldReturn200WhenUserIsDeleted() throws Exception {
        sendJson(delete(DELETE_URL), passwordBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("The user account has been deleted"))
                .andExpect(jsonPath("$.error").doesNotExist());

        verify(service).deleteUserByUsername(ALICE, PASSWORD);
    }

    @Test
    void deleteUser_shouldReturn400WhenPasswordIsWrong() throws Exception {
        WrongPasswordException exception = new WrongPasswordException("The passwords don't match");
        doThrow(exception).when(service).deleteUserByUsername(ALICE, PASSWORD);

        sendJson(delete(DELETE_URL), passwordBody())
                .andExpect(problem(HttpStatus.BAD_REQUEST, exception))
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void updateCurrentUserPassword_shouldReturn400WhenPasswordsAreEqual() throws Exception {
        sendJson(put(PASSWORD_URL).with(csrf()), passwordChange(PASSWORD, PASSWORD))
                .andExpect(validationError())
                .andExpect(jsonPath("$.errors[0].message").value("New password and old password must be different"));

        verifyNoInteractions(service);
    }

    @Test
    void updateCurrentUserPassword_shouldReturn200WhenPasswordIsChanged() throws Exception {
        sendJson(put(PASSWORD_URL).with(csrf()), passwordChange(PASSWORD, NEW_PASSWORD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("The password has been changed successfully. Please log in again."));

        verify(service).updateUserPassword(ALICE, PASSWORD, NEW_PASSWORD);
    }

    @Test
    void updateUserInformation_shouldReturn400WhenNoFieldIsProvided() throws Exception {
        sendJson(patch(INFO_URL).with(csrf()), JsonBody.empty())
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
        sendJson(patch(INFO_URL).with(csrf()), usernameChange("a".repeat(User.USERNAME_MAX_LENGTH)))
                .andExpect(status().isOk());

        verify(service).updateUserInformation(eq(ALICE), any(UpdateUserInformationRequest.class));
    }

    @Test
    void updateUserInformation_shouldReturn200WhenInformationIsUpdated() throws Exception {
        sendJson(patch(INFO_URL).with(csrf()), emailChange("alice.new@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message")
                        .value("The information has been saved. Please log in again to apply changes."));

        verify(service).updateUserInformation(eq(ALICE), any(UpdateUserInformationRequest.class));
    }

    @Test
    void updateUserInformation_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("Email already exists");
        doThrow(exception).when(service).updateUserInformation(eq(ALICE), any(UpdateUserInformationRequest.class));

        sendJson(patch(INFO_URL).with(csrf()), emailChange(email(BOB)))
                .andExpect(problem(HttpStatus.CONFLICT, exception));
    }

    @Test
    void logoutCurrentUser_shouldReturn204() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    @WithAnonymousUser
    void deleteUser_shouldReturn401ForAnonymousUser() throws Exception {
        sendJson(delete(DELETE_URL), passwordBody()).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void updateCurrentUserPassword_shouldReturn401ForAnonymousUser() throws Exception {
        sendJson(put(PASSWORD_URL).with(csrf()), passwordChange(PASSWORD, NEW_PASSWORD))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void updateUserInformation_shouldReturn401ForAnonymousUser() throws Exception {
        sendJson(patch(INFO_URL).with(csrf()), usernameChange("alice_new")).andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void logoutCurrentUser_shouldReturn401ForAnonymousUser() throws Exception {
        mockMvc.perform(post(LOGOUT_URL)).andExpect(status().isUnauthorized());
    }

    private static JsonBody passwordBody() {
        return JsonBody.empty().with("password", PASSWORD);
    }

    private static JsonBody passwordChange(String oldPassword, String newPassword) {
        return JsonBody.empty().with("oldPassword", oldPassword).with("newPassword", newPassword);
    }

    private static JsonBody usernameChange(String username) {
        return JsonBody.empty().with("newUsername", username);
    }

    private static JsonBody emailChange(String email) {
        return JsonBody.empty().with("newEmail", email);
    }
}
