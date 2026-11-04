package com.app.subly.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Email;
import lombok.Getter;

@Getter
public class UpdateUserInformationRequest {

    private String newUsername;
    private boolean usernameProvided = false;

    @Email
    private String newEmail;

    private boolean emailProvided = false;

    @JsonSetter("newUsername")
    public void setNewUsername(String username){
        this.newUsername = username;
        this.usernameProvided = true;
    }

    @JsonSetter("newEmail")
    public void setNewEmail(String email){
        this.newEmail = email;
        this.emailProvided = true;
    }
}
