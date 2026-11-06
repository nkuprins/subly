package com.app.subly.dto.invitation;

import com.app.subly.model.subscription.PendingInvitation;

import java.time.LocalDateTime;

public record InviteResponse(Long id, String username, LocalDateTime invitedAt) {

    public static InviteResponse from(PendingInvitation invitation) {
        return new InviteResponse(
                invitation.getId(),
                invitation.getUser().getUsername(),
                invitation.getInvitedAt());
    }
}
