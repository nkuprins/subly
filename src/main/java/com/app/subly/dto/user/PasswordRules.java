package com.app.subly.dto.user;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

// Idea: create custom annotation for complex password check
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PasswordRules {

    public static final int MIN_LENGTH = 15;

    public static final int MAX_LENGTH = 64;
}
