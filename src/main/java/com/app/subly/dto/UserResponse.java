package com.app.subly.dto;

import com.app.subly.model.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class UserResponse {
    private String username;
    private String email;
    private UserRole role;
}
