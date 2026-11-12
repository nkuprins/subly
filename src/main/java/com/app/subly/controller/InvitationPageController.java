package com.app.subly.controller;

import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.subscription.MemberListResponse;
import com.app.subly.exception.BadRequestException;
import com.app.subly.exception.ConflictException;
import com.app.subly.exception.subscription.MembershipActionForbiddenException;
import com.app.subly.service.InvitationService;
import com.app.subly.service.MembershipService;
import com.app.subly.service.SubscriptionService;
import java.security.Principal;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
        log.info("Preparing invitation page for the subscription id={}", id);
        MemberListResponse members = membershipService.findMembers(principal.getName(), id);
        model.addAttribute("subscriptionId", id);
        model.addAttribute("pending", invitationService.findPending(principal.getName(), id));
        model.addAttribute("members", members.members());
        model.addAttribute("full", members.full());
        log.info("Invitation page is prepared for the subscriptionId={}", id);
        return "invitations";
    }

    @PostMapping("/subscriptions/{id}/invite")
    public ModelAndView invite(@PathVariable long id, Principal principal, @RequestParam String usernameOrEmail) {
        log.info("Sending invite to usernameOrEmail={}, subscriptionId={}", usernameOrEmail, id);
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
        log.info("Preparing participant page for subscriptionId={}", id);
        model.addAttribute("sub", subscriptionService.getActive(principal.getName(), id));
        model.addAttribute(
                "participants",
                membershipService.findMembers(principal.getName(), id).members());
        log.info("Successfully prepared participant page for subscriptionId={}", id);
        return "participants";
    }

    @PostMapping("/subscriptions/{id}/leave")
    public String leaveSubscription(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        log.info("Leaving the group subscription with id={}, username={}", id, principal.getName());
        try {
            membershipService.leave(principal.getName(), id);
            redirectAttributes.addFlashAttribute("notice", "You have left the group.");
            log.info("Successfully removed username={} from the subscriptionId={}", principal.getName(), id);
        } catch (MembershipActionForbiddenException ex) {
            log.error(
                    "Error occurred during leaving username={}, subscriptionId={}: {}",
                    principal.getName(),
                    id,
                    ex.getMessage());
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
        log.info("Kicking username={} from subscriptionId={} membershipId={}", principal.getName(), id, membershipId);
        try {
            membershipService.removeMember(principal.getName(), id, membershipId);
            redirectAttributes.addFlashAttribute("notice", "Member removed successfully.");
            log.info(
                    "Successfully kicked username={} from subscriptionId={} membershipId={}",
                    principal.getName(),
                    id,
                    membershipId);
        } catch (MembershipActionForbiddenException ex) {
            log.error("Error occurred during kicking username={}: {}", principal.getName(), ex.getMessage());
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/subscriptions/" + id + "/invite";
    }

    @PostMapping("/invitations/{invitationId}/accept")
    public String acceptInvitation(
            @PathVariable long invitationId, Principal principal, RedirectAttributes redirectAttributes) {
        log.info("Accepting invitationId={} for username={}", invitationId, principal.getName());
        try {
            invitationService.accept(principal.getName(), invitationId);
            redirectAttributes.addFlashAttribute("notice", "Invitation accepted. The subscription is now in your list");
            log.info("The invitation (invitationId={}) has been accepted", invitationId);
        } catch (ConflictException ex) {
            log.error("Some conflict error has occurred (invitationId={}): {}", invitationId, ex.getMessage());
            redirectAttributes.addFlashAttribute("notice", ex.getMessage());
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/invitations/{invitationId}/decline")
    public String declineInvitation(
            @PathVariable long invitationId, Principal principal, RedirectAttributes redirectAttributes) {
        log.info("Declining the invitationId={} for username={}", invitationId, principal.getName());
        invitationService.decline(principal.getName(), invitationId);
        redirectAttributes.addFlashAttribute("notice", "Invitation declined.");
        log.info("Successfully declined the invitationId={} for username={}", invitationId, principal.getName());
        return "redirect:/dashboard";
    }

    private ModelAndView invitationError(long id, Principal principal, RuntimeException ex, HttpStatus status) {
        log.error("Invitation sending error: {}", ex.getMessage());
        ModelAndView result = new ModelAndView("invitations");
        result.setStatus(status);
        result.addObject("subscriptionId", id);
        result.addObject("pending", invitationService.findPending(principal.getName(), id));
        MemberListResponse members = membershipService.findMembers(principal.getName(), id);
        result.addObject("members", members.members());
        result.addObject("full", members.full());
        result.addObject("error", ex.getMessage());
        return result;
    }
}
