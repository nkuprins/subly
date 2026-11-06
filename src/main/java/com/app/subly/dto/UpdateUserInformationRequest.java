package com.app.subly.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import lombok.Getter;

@Getter
public class UpdateUserInformationRequest {

    private String newUsername;
    private boolean usernameProvided = false;

    @Email
    private String newEmail;

    private boolean emailProvided = false;

    @Size(max = 100)
    private String newName;

    private boolean nameProvided = false;

    private ZoneId newTimeZone;
    private boolean timeZoneProvided = false;

    @JsonSetter("newUsername")
    public void setNewUsername(String username) {
        this.newUsername = username;
        this.usernameProvided = true;
    }

    @JsonSetter("newEmail")
    public void setNewEmail(String email) {
        this.newEmail = email;
        this.emailProvided = true;
    }

    @JsonSetter("newName")
    public void setNewName(String name) {
        this.newName = name;
        this.nameProvided = true;
    }

    @JsonSetter("newTimeZone")
    public void setNewTimeZone(ZoneId timeZone) {
        this.newTimeZone = timeZone;
        this.timeZoneProvided = true;
    }
}
