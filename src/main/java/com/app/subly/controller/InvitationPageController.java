package com.app.subly.controller;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.service.InvitationService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class InvitationPageController {
    private final InvitationService invitationService;

    public InvitationPageController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping("/subscriptions/{id}/invite")
    public String invitePage(@PathVariable long id, Principal principal, Model model) {
        if (principal == null) return "redirect:/login";
        model.addAttribute("subscriptionId", id);
        model.addAttribute("pending", invitationService.findPending(principal.getName(), id));
        return "invitations";
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
            model.addAttribute("error", ex.getMessage());
            return "invitations";
        }
    }
}
