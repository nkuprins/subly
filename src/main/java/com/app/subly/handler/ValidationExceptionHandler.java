package com.app.subly.handler;

import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.Errors;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class ValidationExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleInvalidBody(MethodArgumentNotValidException ex) {
        return validationProblem(invalidFields(ex.getBindingResult()));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleInvalidParameters(HandlerMethodValidationException ex) {
        List<InvalidField> fields = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors errors) {
                // @Valid body
                fields.addAll(invalidFields(errors));
            } else {
                // @PathVariable, @RequestParam
                String name = result.getMethodParameter().getParameterName();
                result.getResolvableErrors()
                        .forEach(error -> fields.add(new InvalidField(name, error.getDefaultMessage())));
            }
        }
        return validationProblem(fields);
    }

    private static List<InvalidField> invalidFields(Errors errors) {
        return errors.getFieldErrors().stream()
                .map(error -> new InvalidField(error.getField(), error.getDefaultMessage()))
                .toList();
    }

    private static ProblemDetail validationProblem(List<InvalidField> fields) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request content.");
        problem.setProperty("errors", fields);
        return problem;
    }

    record InvalidField(String field, String message) {}
}
