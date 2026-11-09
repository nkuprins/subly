package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.app.subly.service.InvitationService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(InvitationController.class)
@WithMockUser(username = ALICE)
class InvitationControllerTest extends AbstractWebMvcTest {

    static final long INVITATION_ID = 5L;
    static final String ACCEPT_URL = "/api/invitations/" + INVITATION_ID + "/accept";
    static final String DECLINE_URL = "/api/invitations/" + INVITATION_ID + "/decline";

    @MockitoBean
    InvitationService invitationService;

    @Test
    void accept_shouldReturn204ForAuthenticatedUser() throws Exception {
        mockMvc.perform(post(ACCEPT_URL)).andExpect(status().isNoContent());

        verify(invitationService).accept(ALICE, INVITATION_ID);
    }

    @Test
    void decline_shouldReturn204ForAuthenticatedUser() throws Exception {
        mockMvc.perform(post(DECLINE_URL)).andExpect(status().isNoContent());

        verify(invitationService).decline(ALICE, INVITATION_ID);
    }

    @Test
    @WithAnonymousUser
    void accept_shouldReturn401ForAnonymousUser() throws Exception {
        mockMvc.perform(post(ACCEPT_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

        verifyNoInteractions(invitationService);
    }

    @Test
    @WithAnonymousUser
    void decline_shouldReturn401ForAnonymousUser() throws Exception {
        mockMvc.perform(post(DECLINE_URL).accept(MediaType.APPLICATION_JSON)).andExpect(status().isUnauthorized());

        verifyNoInteractions(invitationService);
    }
}
