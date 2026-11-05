package com.app.subly.controller;

import com.app.subly.dto.invitation.InvitationResponse;
import com.app.subly.dto.invitation.InviteRequest;
import com.app.subly.dto.invitation.ReceivedInvitationResponse;
import com.app.subly.services.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService service;

    @PostMapping("/subscriptions/{id}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public InvitationResponse invite(Principal principal, @PathVariable long id,
                                     @Valid @RequestBody InviteRequest request) {
        return service.invite(principal.getName(), id, request);
    }

    @GetMapping("/subscriptions/{id}/invitations")
    public List<InvitationResponse> listPending(Principal principal, @PathVariable long id) {
        return service.findPending(principal.getName(), id);
    }

    @GetMapping("/invitations")
    public List<ReceivedInvitationResponse> listReceived(Principal principal) {
        return service.findReceived(principal.getName());
    }
}
