package com.app.subly.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.app.subly.config.SecurityConfig;
import com.app.subly.dto.UpdateUserInformationRequest;
import com.app.subly.exception.UserAlreadyExistsException;
import com.app.subly.exception.WrongPasswordException;
import com.app.subly.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
public class UserControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService service;

    @Test
    @WithMockUser(username = "andris123")
    void deleteUser_ShouldReturnOkWhenUserIsDeleted() throws Exception {

        mockMvc.perform(delete("/users/delete/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "password": "largepassword123456"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(false))
                .andExpect(jsonPath("$.message").value("The user account has been deleted"));

        verify(service).deleteUserByUsername("andris123", "largepassword123456");
    }

    @Test
    @WithMockUser(username = "andris123")
    void deleteUser_ShouldReturnBadRequestWhenPasswordIsWrong() throws Exception {

        doThrow(new WrongPasswordException("The passwords don't match"))
                .when(service)
                .deleteUserByUsername("andris123", "largepassword123456");

        mockMvc.perform(delete("/users/delete/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "password": "largepassword123456"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").value("The passwords don't match"));
    }

    @Test
    @WithMockUser(username = "andris123")
    void updatePassword_ShouldReturnBadRequestWhenPasswordsAreEqual() throws Exception {

        mockMvc.perform(put("/users/update/password/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "oldPassword": "largepassword123456",
                            "newPassword": "largepassword123456"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("New password and old password must be different"));

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "andris123")
    void updatePassword_ShouldReturnOkWhenPasswordIsChanged() throws Exception {
        mockMvc.perform(put("/users/update/password/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "oldPassword": "largepassword123456",
                            "newPassword": "superlargepassword123456"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(false));

        verify(service).updateUserPassword("andris123", "largepassword123456", "superlargepassword123456");
    }

    @Test
    @WithMockUser(username = "andris123")
    void updateInformation_ShouldReturnBadRequestWhenNoFieldsProvided() throws Exception {
        mockMvc.perform(patch("/users/update/info/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("You must provide at least one property to change."));

        verifyNoInteractions(service);
    }

    @Test
    @WithMockUser(username = "andris123")
    void updateInformation_ShouldReturnOkWhenInformationIsUpdated() throws Exception {

        mockMvc.perform(patch("/users/update/info/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "newEmail": "new_email@gmail.com"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.error").value(false));

        verify(service).updateUserInformation(eq("andris123"), any(UpdateUserInformationRequest.class));
    }

    @Test
    @WithMockUser(username = "andris123")
    void updateInformation_ShouldReturnBadRequestWhenEmailAlreadyExists() throws Exception {

        doThrow(new UserAlreadyExistsException("Email already exists"))
                .when(service)
                .updateUserInformation(eq("andris123"), any(UpdateUserInformationRequest.class));

        mockMvc.perform(patch("/users/update/info/me")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        {
                            "newEmail": "existing@gmail.com"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @WithMockUser(username = "andris123")
    void logout_ShouldReturnNoContent() throws Exception {
        mockMvc.perform(post("/users/logout/me"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void delete_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(delete("/users/delete/me").content("""
                        {
                            "password": "somerealpassword123456"
                        }
                        """).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateCurrentUserPassword_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(put("/users/update/password/me")
                        .with(csrf())
                        .content("""
                        {
                            "oldPassword": "somerealpassword123456",
                            "newPassword" : "newsomerealpassword123456"
                        }
                        """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateUserInformation_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(patch("/users/update/info/me")
                        .with(csrf())
                        .content("""
                        {
                            "newUsername": "someusername",
                        }
                        """)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutCurrentUser_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/users/logout/me")).andExpect(status().isUnauthorized());
    }
}
