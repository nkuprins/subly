package com.app.subly.dto.invitation;

import com.app.subly.entity.invitation.PendingInvitation;
import java.time.Instant;

public record InviteResponse(String username, Instant invitedAt) {

    public static InviteResponse from(PendingInvitation invitation) {
        return new InviteResponse(invitation.getUser().getUsername(), invitation.getInvitedAt());
    }
}
