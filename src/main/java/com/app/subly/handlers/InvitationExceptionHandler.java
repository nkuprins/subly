package com.app.subly.handlers;

import com.app.subly.exceptions.invitation.AlreadyInvitedException;
import com.app.subly.exceptions.invitation.AlreadyMemberException;
import com.app.subly.exceptions.invitation.InvitedUserNotFoundException;
import com.app.subly.exceptions.invitation.SelfInvitationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class InvitationExceptionHandler {

    @ExceptionHandler({InvitedUserNotFoundException.class, SelfInvitationException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler({AlreadyMemberException.class, AlreadyInvitedException.class})
    public ProblemDetail handleConflict(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }
}
