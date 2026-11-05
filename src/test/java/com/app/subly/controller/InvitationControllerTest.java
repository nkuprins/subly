package com.app.subly.controller;

import com.app.subly.config.SecurityConfig;
import com.app.subly.services.InvitationService;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.then;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvitationController.class)
@Import(SecurityConfig.class)
class InvitationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @Test
    void authenticatedUserCanAcceptInvitation() throws Exception {
        mockMvc.perform(
                post("/api/invitations/5/accept")
                        .with(user("bob"))).
                andExpect(status().isNoContent());

        then(invitationService).should().accept("bob", 5L);
    }

    @Test
    void authenticatedUserCanDeclineInvitation() throws Exception {
        mockMvc.perform(
                post("/api/invitations/5/decline")
                        .with(user("bob")))
                .andExpect(status().isNoContent());

        then(invitationService).should().decline("bob", 5L);
    }

    @Test
    void anonymousUserCannotAcceptInvitation() throws Exception {
        mockMvc.perform(
                post("/api/invitations/5/accept")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void anonymousUserCannotDeclineInvitation() throws Exception {
        mockMvc.perform(
                post("/api/invitations/5/decline")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }
}
