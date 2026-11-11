package com.app.subly.controller;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.subscription.ActiveSubscriptionResponse;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.exception.BadRequestException;
import com.app.subly.exception.ConflictException;
import com.app.subly.exception.subscription.MembershipActionForbiddenException;
import com.app.subly.service.InvitationService;
import com.app.subly.service.MembershipService;
import com.app.subly.service.SubscriptionService;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InvitationPageController {
    private final InvitationService invitationService;
    private final SubscriptionService subscriptionService;
    private final MembershipService membershipService;

    public InvitationPageController(
            InvitationService invitationService,
            SubscriptionService subscriptionService,
            MembershipService membershipService) {
        this.invitationService = invitationService;
        this.subscriptionService = subscriptionService;
        this.membershipService = membershipService;
    }

    @GetMapping("/subscriptions/{id}/invite")
    public String invitePage(@PathVariable long id, Principal principal, Model model) {
        var members = membershipService.findMembers(principal.getName(), id);
        int max = subscriptionService.findActive(principal.getName(), id)
                .map(SubscriptionResponse::maxParticipants)
                .orElse(1);
        model.addAttribute("subscriptionId", id);
        model.addAttribute("pending", invitationService.findPending(principal.getName(), id));
        model.addAttribute("members", members);
        model.addAttribute("full", members.size() >= max);
        return "invitations";
    }

    @PostMapping("/subscriptions/{id}/invite")
    public ModelAndView invite(@PathVariable long id, Principal principal, @RequestParam String usernameOrEmail) {
        try {
            invitationService.invite(principal.getName(), id, new InviteRequest(usernameOrEmail));
            return new ModelAndView("redirect:/subscriptions/" + id + "/invite");
        } catch (BadRequestException ex) {
            return invitationError(id, principal, ex, HttpStatus.BAD_REQUEST);
        } catch (ConflictException ex) {
            return invitationError(id, principal, ex, HttpStatus.CONFLICT);
        }
    }

    @GetMapping("/subscriptions/{id}/participants")
    public String participants(@PathVariable Long id, Principal principal, Model model) {
        var sub = subscriptionService.findActive(principal.getName(), id).orElse(null);
        if (sub == null) return "redirect:/subscriptions";
        model.addAttribute("sub", sub);
        model.addAttribute("participants", membershipService.findParticipants(principal.getName(), id));
        return "participants";
    }

    @PostMapping("/subscriptions/{id}/leave")
    public String leaveSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            membershipService.leave(principal.getName(), id);
            redirectAttributes.addFlashAttribute("notice", "You have left the group.");
        } catch (MembershipActionForbiddenException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/members/{membershipId}/kick")
    public String kickMember(
            @PathVariable long id,
            @PathVariable long membershipId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        try {
            membershipService.removeMember(principal.getName(), id, membershipId);
            redirectAttributes.addFlashAttribute("notice", "Member removed successfully.");
        } catch (MembershipActionForbiddenException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/subscriptions/" + id + "/invite";
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public String acceptInvitation(
            @PathVariable long invitationId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            invitationService.accept(principal.getName(), invitationId);
            redirectAttributes.addFlashAttribute("notice", "Invitation accepted. The subscription is now in your list");
        } catch (ConflictException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public String declineInvitation(
            @PathVariable long invitationId, Principal principal, RedirectAttributes redirectAttributes) {
        invitationService.decline(principal.getName(), invitationId);
        redirectAttributes.addFlashAttribute("notice", "Invitation declined.");
        return "redirect:/dashboard";
    }

    private ModelAndView invitationError(long id, Principal principal, RuntimeException ex, HttpStatus status) {
        ModelAndView result = new ModelAndView("invitations");
        result.setStatus(status);
        result.addObject("subscriptionId", id);
        result.addObject("pending", invitationService.findPending(principal.getName(), id));
        var members = membershipService.findMembers(principal.getName(), id);
        result.addObject("members", members);
        result.addObject("full", isSubscriptionFull(principal.getName(), id, members.size()));
        result.addObject("error", ex.getMessage());
        return result;
    }

    private boolean isSubscriptionFull(String username, long id, int memberCount) {
        int max = subscriptionService.findActive(username).stream()
                .map(ActiveSubscriptionResponse::subscription)
                .filter(s -> s.id().equals(id))
                .findFirst()
                .map(com.app.subly.dto.subscription.SubscriptionResponse::maxParticipants)
                .orElse(1);
        return memberCount >= max;
    }
}
