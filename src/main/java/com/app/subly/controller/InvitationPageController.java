package com.app.subly.controller;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.subscription.SubscriptionResponse;
import com.app.subly.service.InvitationService;
import com.app.subly.service.MembershipService;
import com.app.subly.service.SubscriptionService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
        try {
            var members = membershipService.findMembers(principal.getName(), id);
            int max = subscriptionService
                    .findActive(principal.getName(), id)
                    .map(SubscriptionResponse::maxParticipants)
                    .orElse(1);
            model.addAttribute("subscriptionId", id);
            model.addAttribute("pending", invitationService.findPending(principal.getName(), id));
            model.addAttribute("members", members);
            model.addAttribute("full", members.size() >= max);
            return "invitations";
        } catch (RuntimeException ex) {
            return "redirect:/subscriptions";
        }
    }

    @PostMapping("/subscriptions/{id}/invite")
    public String invite(
            @PathVariable long id, Principal principal, @RequestParam String usernameOrEmail, Model model) {
        try {
            invitationService.invite(principal.getName(), id, new InviteRequest(usernameOrEmail));
            return "redirect:/subscriptions/" + id + "/invite";
        } catch (RuntimeException ex) {
            model.addAttribute("subscriptionId", id);
            model.addAttribute("pending", invitationService.findPending(principal.getName(), id));
            model.addAttribute("members", membershipService.findMembers(principal.getName(), id));
            model.addAttribute("error", ex.getMessage());
            return "invitations";
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
        } catch (RuntimeException ex) {
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
        } catch (RuntimeException ex) {
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
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public String declineInvitation(
            @PathVariable long invitationId, Principal principal, RedirectAttributes redirectAttributes) {
        try {
            invitationService.decline(principal.getName(), invitationId);
            redirectAttributes.addFlashAttribute("notice", "Invitation declined.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/dashboard";
    }
}
