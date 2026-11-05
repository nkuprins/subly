package com.app.subly.handlers;

import com.app.subly.exceptions.subscription.CatalogServiceReadOnlyException;
import com.app.subly.exceptions.subscription.CategoryNotFoundException;
import com.app.subly.exceptions.subscription.CatalogServiceNotFoundException;
import com.app.subly.exceptions.subscription.PaymentDateInPastException;
import com.app.subly.exceptions.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exceptions.subscription.SubscriptionNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class SubscriptionExceptionHandler {

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ProblemDetail handleNotFound(SubscriptionNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(SubscriptionAccessDeniedException.class)
    public ProblemDetail handleAccessDenied(SubscriptionAccessDeniedException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({CategoryNotFoundException.class, CatalogServiceNotFoundException.class,
            CatalogServiceReadOnlyException.class, PaymentDateInPastException.class})
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
