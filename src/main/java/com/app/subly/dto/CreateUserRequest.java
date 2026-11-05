package com.app.subly.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.ZoneId;

@AllArgsConstructor
@Getter
public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    @Size(max=100)
    private String name;

    @Size(min = 15, max = 64)
    private String password;

    @Email
    @NotBlank
    private String email;

    private ZoneId timeZone;
}
