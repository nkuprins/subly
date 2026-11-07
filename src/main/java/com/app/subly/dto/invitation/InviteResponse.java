package com.app.subly.dto.invitation;

import com.app.subly.model.subscription.PendingInvitation;
import java.time.Instant;

public record InviteResponse(Long id, String username, Instant invitedAt) {

    public static InviteResponse from(PendingInvitation invitation) {
        return new InviteResponse(invitation.getId(), invitation.getUser().getUsername(), invitation.getInvitedAt());
    }
}
