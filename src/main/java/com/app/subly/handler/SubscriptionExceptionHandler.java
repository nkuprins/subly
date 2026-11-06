package com.app.subly.handler;

import com.app.subly.exception.subscription.CatalogProviderNotFoundException;
import com.app.subly.exception.subscription.CatalogProviderReadOnlyException;
import com.app.subly.exception.subscription.CategoryNotFoundException;
import com.app.subly.exception.subscription.PaymentDateInPastException;
import com.app.subly.exception.subscription.SubscriptionAccessDeniedException;
import com.app.subly.exception.subscription.SubscriptionNotFoundException;
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

    @ExceptionHandler({
        CategoryNotFoundException.class,
        CatalogProviderNotFoundException.class,
        CatalogProviderReadOnlyException.class,
        PaymentDateInPastException.class
    })
    public ProblemDetail handleBadRequest(RuntimeException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }
}
