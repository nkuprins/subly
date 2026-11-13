package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.app.subly.dto.user.PasswordRules;
import com.app.subly.entity.user.User;
import com.app.subly.exception.user.UserAlreadyExistsException;
import com.app.subly.service.UserService;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(UserPageController.class)
@WithMockUser(username = ALICE)
class UserPageControllerTest extends AbstractWebMvcTest {

    private static final String REGISTER_URL = "/register";
    private static final String USERNAME = "bob";
    private static final String NAME = "Bob";
    private static final String EMAIL = "bob@example.com";
    private static final String PASSWORD = "longsecurepassword123";

    @MockitoBean
    private UserService userService;

    @Test
    @WithAnonymousUser
    void login_shouldRenderLoginPage() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithAnonymousUser
    void accessDenied_shouldRenderAccessDeniedPage() throws Exception {
        mockMvc.perform(get("/access-denied")).andExpect(status().isOk()).andExpect(view().name("access-denied"));

        verifyNoInteractions(userService);
    }

    @Test
    @WithAnonymousUser
    void register_shouldRenderRegistrationPage() throws Exception {
        mockMvc.perform(get(REGISTER_URL)).andExpect(status().isOk()).andExpect(view().name("register"));

        verifyNoInteractions(userService);
    }

    @Test
    void returnEditPage_shouldDisplayCurrentUserAndTimeZones() throws Exception {
        User user = mock(User.class);
        when(user.getUsername()).thenReturn(ALICE);
        when(user.getTimeZone()).thenReturn(ZoneId.of("Europe/Riga"));
        when(userService.getUser(ALICE)).thenReturn(user);

        mockMvc.perform(get("/edit/me"))
                .andExpect(status().isOk())
                .andExpect(view().name("edit-user"))
                .andExpect(model().attribute("user", user))
                .andExpect(model().attribute("timeZones", hasItem("Europe/Riga")));

        verify(userService).getUser(ALICE);
    }

    @Test
    @WithAnonymousUser
    void returnEditPage_shouldRejectAnonymousUser() throws Exception {
        mockMvc.perform(get("/edit/me"))
                .andExpect(result -> assertTrue(result.getResponse().getStatus() == 302
                        || result.getResponse().getStatus() == 401));

        verifyNoInteractions(userService);
    }

    @Test
    @WithAnonymousUser
    void createAccount_shouldRegisterUserAndRedirectToLogin() throws Exception {
        register(USERNAME, NAME, EMAIL, PASSWORD, PASSWORD)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).create(USERNAME, NAME, PASSWORD, EMAIL, null);
    }

    @Test
    @WithAnonymousUser
    void createAccount_shouldRejectMissingCsrfToken() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .param("username", USERNAME)
                        .param("name", NAME)
                        .param("email", EMAIL)
                        .param("password", PASSWORD)
                        .param("confirmPassword", PASSWORD))
                .andExpect(status().isForbidden());

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldReturn400WhenRequiredParameterIsMissing() throws Exception {
        mockMvc.perform(post(REGISTER_URL)
                        .with(csrf())
                        .param("username", USERNAME)
                        .param("name", NAME)
                        .param("email", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectBlankUsername() throws Exception {
        register(" ", NAME, EMAIL, PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("registrationError", "All fields are required"));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectBlankName() throws Exception {
        register(USERNAME, " ", EMAIL, PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(model().attribute("registrationError", "All fields are required"));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectBlankEmail() throws Exception {
        register(USERNAME, NAME, " ", PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(model().attribute("registrationError", "All fields are required"));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectBlankPassword() throws Exception {
        register(USERNAME, NAME, EMAIL, " ", " ")
                .andExpect(status().isOk())
                .andExpect(model().attribute("registrationError", "All fields are required"));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectUsernameLongerThanMaximum() throws Exception {
        register("a".repeat(User.USERNAME_MAX_LENGTH + 1), NAME, EMAIL, PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                                "registrationError",
                                "Username must be " + User.USERNAME_MAX_LENGTH + " characters or fewer"));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectNameLongerThanMaximum() throws Exception {
        register(USERNAME, "a".repeat(User.NAME_MAX_LENGTH + 1), EMAIL, PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(model().attribute(
                                "registrationError", "Name must be " + User.NAME_MAX_LENGTH + " characters or fewer"));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectPasswordShorterThanMinimum() throws Exception {
        String password = "a".repeat(PasswordRules.MIN_LENGTH - 1);

        register(USERNAME, NAME, EMAIL, password, password)
                .andExpect(status().isOk())
                .andExpect(model().attribute("registrationError", passwordLengthMessage()));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectPasswordLongerThanMaximum() throws Exception {
        String password = "a".repeat(PasswordRules.MAX_LENGTH + 1);

        register(USERNAME, NAME, EMAIL, password, password)
                .andExpect(status().isOk())
                .andExpect(model().attribute("registrationError", passwordLengthMessage()));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldRejectDifferentPasswords() throws Exception {
        register(USERNAME, NAME, EMAIL, PASSWORD, PASSWORD + "wrong")
                .andExpect(status().isOk())
                .andExpect(model().attribute("registrationError", "Passwords do not match"));

        verifyNoInteractions(userService);
    }

    @Test
    void createAccount_shouldShowErrorWhenUserAlreadyExists() throws Exception {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("Username or email already exists");
        doThrow(exception).when(userService).create(USERNAME, NAME, PASSWORD, EMAIL, null);

        register(USERNAME, NAME, EMAIL, PASSWORD, PASSWORD)
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attribute("registrationError", exception.getMessage()));

        verify(userService).create(USERNAME, NAME, PASSWORD, EMAIL, null);
    }

    @Test
    void createAccount_shouldAcceptUsernameAtMaximumLength() throws Exception {
        String username = "a".repeat(User.USERNAME_MAX_LENGTH);

        register(username, NAME, EMAIL, PASSWORD, PASSWORD)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered"));

        verify(userService).create(username, NAME, PASSWORD, EMAIL, null);
    }

    @Test
    void createAccount_shouldAcceptNameAtMaximumLength() throws Exception {
        String name = "a".repeat(User.NAME_MAX_LENGTH);

        register(USERNAME, name, EMAIL, PASSWORD, PASSWORD).andExpect(status().is3xxRedirection());

        verify(userService).create(USERNAME, name, PASSWORD, EMAIL, null);
    }

    @Test
    void createAccount_shouldAcceptPasswordAtMinimumLength() throws Exception {
        String password = "a".repeat(PasswordRules.MIN_LENGTH);

        register(USERNAME, NAME, EMAIL, password, password).andExpect(status().is3xxRedirection());

        verify(userService).create(USERNAME, NAME, password, EMAIL, null);
    }

    @Test
    void createAccount_shouldAcceptPasswordAtMaximumLength() throws Exception {
        String password = "a".repeat(PasswordRules.MAX_LENGTH);

        register(USERNAME, NAME, EMAIL, password, password).andExpect(status().is3xxRedirection());

        verify(userService).create(USERNAME, NAME, password, EMAIL, null);
    }

    private ResultActions register(String username, String name, String email, String password, String confirmPassword)
            throws Exception {
        return mockMvc.perform(post(REGISTER_URL)
                .with(csrf())
                .param("username", username)
                .param("name", name)
                .param("email", email)
                .param("password", password)
                .param("confirmPassword", confirmPassword));
    }

    private static String passwordLengthMessage() {
        return "Password must be between " + PasswordRules.MIN_LENGTH + " and " + PasswordRules.MAX_LENGTH
                + " characters";
    }
}
