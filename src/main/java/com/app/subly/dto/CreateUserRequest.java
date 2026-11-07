package com.app.subly.dto;

import com.app.subly.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Size(max = User.NAME_MAX_LENGTH)
    private String name;

    @Size(min = PasswordRules.MIN_LENGTH, max = PasswordRules.MAX_LENGTH)
    private String password;

    @Email
    @NotBlank
    private String email;

    private ZoneId timeZone;
}
