package com.app.subly.controller;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.service.InvitationService;
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

    public InvitationPageController(InvitationService invitationService, SubscriptionService subscriptionService) {
        this.invitationService = invitationService;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/subscriptions/{id}/invite")
    public String invitePage(@PathVariable long id, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        try {
            model.addAttribute("subscriptionId", id);
            model.addAttribute("pending", invitationService.findPending(principal.getName(), id));
            model.addAttribute("members", subscriptionService.findMembers(principal.getName(), id));
            return "invitations";
        } catch (RuntimeException ex) {
            return "redirect:/subscriptions";
        }
    }

    @PostMapping("/subscriptions/{id}/invite")
    public String invite(
            @PathVariable long id, Principal principal, @RequestParam String usernameOrEmail, Model model) {
        if (principal == null) return "redirect:/login";
        try {
            invitationService.invite(principal.getName(), id, new InviteRequest(usernameOrEmail));
            return "redirect:/subscriptions/" + id + "/invite";
        } catch (RuntimeException ex) {
            model.addAttribute("subscriptionId", id);
            model.addAttribute("pending", invitationService.findPending(principal.getName(), id));
            model.addAttribute("members", subscriptionService.findMembers(principal.getName(), id));
            model.addAttribute("error", ex.getMessage());
            return "invitations";
        }
    }

    @PostMapping("/subscriptions/{id}/members/{membershipId}/kick")
    public String kickMember(
            @PathVariable long id,
            @PathVariable long membershipId,
            Principal principal,
            RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
        try {
            subscriptionService.removeMember(principal.getName(), id, membershipId);
            redirectAttributes.addFlashAttribute("notice", "Member removed successfully.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/subscriptions/" + id + "/invite";
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public String acceptInvitation(
            @PathVariable long invitationId, Principal principal, RedirectAttributes redirectAttributes) {
        if (principal == null) return "redirect:/login";
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
        if (principal == null) return "redirect:/login";
        try {
            invitationService.decline(principal.getName(), invitationId);
            redirectAttributes.addFlashAttribute("notice", "Invitation declined.");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/dashboard";
    }
}
