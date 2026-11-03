package com.app.subly.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class CreateUserRequest {

    @NotBlank
    String username;

    @Size(min = 15, max = 64)
    String password;

    @Email
    @NotBlank
    String email;
}
