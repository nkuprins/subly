package com.app.subly.controller;

import static com.app.subly.testdata.TestUsers.ALICE;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.subscription.MemberListResponse;
import com.app.subly.dto.subscription.SubscriptionMemberResponse;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.enums.BillingCycle;
import com.app.subly.enums.Currency;
import com.app.subly.enums.MembershipRole;
import com.app.subly.exception.invitation.AlreadyInvitedException;
import com.app.subly.exception.invitation.SelfInvitationException;
import com.app.subly.exception.subscription.MembershipActionForbiddenException;
import com.app.subly.service.InvitationService;
import com.app.subly.service.MembershipService;
import com.app.subly.service.SubscriptionService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(InvitationPageController.class)
@WithMockUser(username = ALICE)
class InvitationPageControllerTest extends AbstractWebMvcTest {

    private static final long SUBSCRIPTION_ID = 7L;
    private static final long INVITATION_ID = 11L;
    private static final long MEMBERSHIP_ID = 13L;
    private static final String INVITE_URL = "/subscriptions/" + SUBSCRIPTION_ID + "/invite";
    private static final String INVITED_USER = "bob";
    private static final String ERROR_MESSAGE = "The requested invitation action is not allowed";
    private static final List<SubscriptionMemberResponse> MEMBERS =
            List.of(new SubscriptionMemberResponse(1L, ALICE, MembershipRole.OWNER, BigDecimal.ONE));
    private static final MemberListResponse MEMBER_LIST = new MemberListResponse(MEMBERS, false);

    @MockitoBean
    InvitationService invitationService;

    @MockitoBean
    SubscriptionService subscriptionService;

    @MockitoBean
    MembershipService membershipService;

    @Test
    void invitePage_shouldReturnInvitationPageWithModelAttributes() throws Exception {
        when(membershipService.findMembers(ALICE, SUBSCRIPTION_ID)).thenReturn(MEMBER_LIST);
        when(invitationService.findPending(ALICE, SUBSCRIPTION_ID)).thenReturn(List.of());

        mockMvc.perform(get(INVITE_URL))
                .andExpect(status().isOk())
                .andExpect(view().name("invitations"))
                .andExpect(model().attribute("subscriptionId", SUBSCRIPTION_ID))
                .andExpect(model().attribute("pending", List.of()))
                .andExpect(model().attribute("members", MEMBERS))
                .andExpect(model().attribute("full", false));

        verify(membershipService).findMembers(ALICE, SUBSCRIPTION_ID);
        verify(invitationService).findPending(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void invite_shouldRedirectToInvitationPageWhenInvitationIsSent() throws Exception {
        mockMvc.perform(post(INVITE_URL).with(csrf()).param("usernameOrEmail", INVITED_USER))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:" + INVITE_URL));

        verify(invitationService).invite(ALICE, SUBSCRIPTION_ID, new InviteRequest(INVITED_USER));
    }

    @Test
    void invite_shouldReturn400WithInvitationPageWhenRequestIsInvalid() throws Exception {
        SelfInvitationException exception = new SelfInvitationException();
        doThrow(exception).when(invitationService).invite(ALICE, SUBSCRIPTION_ID, new InviteRequest(INVITED_USER));
        when(invitationService.findPending(ALICE, SUBSCRIPTION_ID)).thenReturn(List.of());
        when(membershipService.findMembers(ALICE, SUBSCRIPTION_ID)).thenReturn(MEMBER_LIST);

        mockMvc.perform(post(INVITE_URL).with(csrf()).param("usernameOrEmail", INVITED_USER))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("invitations"))
                .andExpect(model().attribute("subscriptionId", SUBSCRIPTION_ID))
                .andExpect(model().attribute("pending", List.of()))
                .andExpect(model().attribute("members", MEMBERS))
                .andExpect(model().attribute("full", false))
                .andExpect(model().attribute("error", exception.getMessage()));
    }

    @Test
    void invite_shouldReturn409WithInvitationPageWhenInvitationConflicts() throws Exception {
        AlreadyInvitedException exception = new AlreadyInvitedException(INVITED_USER, SUBSCRIPTION_ID);
        doThrow(exception).when(invitationService).invite(ALICE, SUBSCRIPTION_ID, new InviteRequest(INVITED_USER));
        when(invitationService.findPending(ALICE, SUBSCRIPTION_ID)).thenReturn(List.of());
        when(membershipService.findMembers(ALICE, SUBSCRIPTION_ID)).thenReturn(MEMBER_LIST);

        mockMvc.perform(post(INVITE_URL).with(csrf()).param("usernameOrEmail", INVITED_USER))
                .andExpect(status().is(HttpStatus.CONFLICT.value()))
                .andExpect(view().name("invitations"))
                .andExpect(model().attribute("error", exception.getMessage()));
    }

    @Test
    void participants_shouldReturnParticipantPageWithSubscriptionAndMembers() throws Exception {
        SubscriptionResponse subscription = new SubscriptionResponse(
                SUBSCRIPTION_ID,
                "Music",
                BigDecimal.TEN,
                Currency.EUR,
                BillingCycle.MONTHLY,
                LocalDate.of(2026, 11, 12),
                false,
                2);
        when(subscriptionService.getActive(ALICE, SUBSCRIPTION_ID)).thenReturn(subscription);
        when(membershipService.findMembers(ALICE, SUBSCRIPTION_ID)).thenReturn(MEMBER_LIST);

        mockMvc.perform(get("/subscriptions/{id}/participants", SUBSCRIPTION_ID))
                .andExpect(status().isOk())
                .andExpect(view().name("participants"))
                .andExpect(model().attribute("sub", subscription))
                .andExpect(model().attribute("participants", MEMBERS));

        verify(subscriptionService).getActive(ALICE, SUBSCRIPTION_ID);
        verify(membershipService).findMembers(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void leaveSubscription_shouldRedirectWithSuccessNoticeWhenMemberLeaves() throws Exception {
        mockMvc.perform(post("/subscriptions/{id}/leave", SUBSCRIPTION_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions"))
                .andExpect(flash().attribute("notice", "You have left the group."));

        verify(membershipService).leave(ALICE, SUBSCRIPTION_ID);
    }

    @Test
    void leaveSubscription_shouldRedirectWithErrorNoticeWhenActionIsForbidden() throws Exception {
        doThrow(new MembershipActionForbiddenException(ERROR_MESSAGE))
                .when(membershipService)
                .leave(ALICE, SUBSCRIPTION_ID);

        mockMvc.perform(post("/subscriptions/{id}/leave", SUBSCRIPTION_ID).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/subscriptions"))
                .andExpect(flash().attribute("notice", ERROR_MESSAGE));
    }

    @Test
    void kickMember_shouldRedirectWithSuccessNoticeWhenMemberIsRemoved() throws Exception {
        mockMvc.perform(post("/subscriptions/{id}/members/{membershipId}/kick", SUBSCRIPTION_ID, MEMBERSHIP_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:" + INVITE_URL))
                .andExpect(flash().attribute("notice", "Member removed successfully."));

        verify(membershipService).removeMember(ALICE, SUBSCRIPTION_ID, MEMBERSHIP_ID);
    }

    @Test
    void kickMember_shouldRedirectWithErrorNoticeWhenActionIsForbidden() throws Exception {
        doThrow(new MembershipActionForbiddenException(ERROR_MESSAGE))
                .when(membershipService)
                .removeMember(ALICE, SUBSCRIPTION_ID, MEMBERSHIP_ID);

        mockMvc.perform(post("/subscriptions/{id}/members/{membershipId}/kick", SUBSCRIPTION_ID, MEMBERSHIP_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:" + INVITE_URL))
                .andExpect(flash().attribute("notice", ERROR_MESSAGE));
    }

    @Test
    void acceptInvitation_shouldRedirectWithSuccessNoticeWhenInvitationIsAccepted() throws Exception {
        mockMvc.perform(post("/invitations/{invitationId}/accept", INVITATION_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"))
                .andExpect(flash().attribute("notice", "Invitation accepted. The subscription is now in your list"));

        verify(invitationService).accept(ALICE, INVITATION_ID);
    }

    @Test
    void acceptInvitation_shouldRedirectWithErrorNoticeWhenInvitationConflicts() throws Exception {
        AlreadyInvitedException exception = new AlreadyInvitedException(INVITED_USER, SUBSCRIPTION_ID);
        doThrow(exception).when(invitationService).accept(ALICE, INVITATION_ID);

        mockMvc.perform(post("/invitations/{invitationId}/accept", INVITATION_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"))
                .andExpect(flash().attribute("notice", exception.getMessage()));
    }

    @Test
    void declineInvitation_shouldRedirectWithSuccessNoticeWhenInvitationIsDeclined() throws Exception {
        mockMvc.perform(post("/invitations/{invitationId}/decline", INVITATION_ID)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/dashboard"))
                .andExpect(flash().attribute("notice", "Invitation declined."));

        verify(invitationService).decline(ALICE, INVITATION_ID);
    }
}
