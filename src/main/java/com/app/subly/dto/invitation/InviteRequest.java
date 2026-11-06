package com.app.subly.dto.invitation;

import jakarta.validation.constraints.NotBlank;

public record InviteRequest(@NotBlank String usernameOrEmail) {}
