package com.app.subly.dto.user;

import com.app.subly.entity.user.User;
import com.fasterxml.jackson.annotation.JsonSetter;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.time.ZoneId;
import lombok.Getter;

@Getter
public class UpdateUserInformationRequest {

    @Size(max = User.USERNAME_MAX_LENGTH)
    private String newUsername;

    private boolean usernameProvided = false;

    @Email
    private String newEmail;

    private boolean emailProvided = false;

    @Size(max = User.NAME_MAX_LENGTH)
    private String newName;

    private boolean nameProvided = false;

    private ZoneId newTimeZone;
    private boolean timeZoneProvided = false;

    @AssertTrue(message = "You must provide at least one property to change.")
    public boolean isAnyFieldProvided() {
        return usernameProvided || emailProvided || nameProvided || timeZoneProvided;
    }

    @JsonSetter("newUsername")
    public void setNewUsername(String username) {
        newUsername = username;
        usernameProvided = true;
    }

    @JsonSetter("newEmail")
    public void setNewEmail(String email) {
        newEmail = email;
        emailProvided = true;
    }

    @JsonSetter("newName")
    public void setNewName(String name) {
        newName = name;
        nameProvided = true;
    }

    @JsonSetter("newTimeZone")
    public void setNewTimeZone(ZoneId timeZone) {
        newTimeZone = timeZone;
        timeZoneProvided = true;
    }
}
