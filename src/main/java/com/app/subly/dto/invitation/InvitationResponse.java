package com.app.subly.dto.invitation;

import com.app.subly.models.subscription.PendingInvitation;

import java.time.LocalDateTime;

public record InvitationResponse(Long id, String username, LocalDateTime invitedAt) {

    public static InvitationResponse from(PendingInvitation invitation) {
        return new InvitationResponse(
                invitation.getId(),
                invitation.getUser().getUsername(),
                invitation.getInvitedAt());
    }
}
