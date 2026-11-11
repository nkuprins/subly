package com.app.subly.handler;

import com.app.subly.exception.BadRequestException;
import com.app.subly.exception.ConflictException;
import com.app.subly.exception.ForbiddenException;
import com.app.subly.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice(annotations = Controller.class)
public class PageControllerHandler {

    private final String errorPageTemplate = "error-page";

    @ExceptionHandler(BadRequestException.class)
    public ModelAndView handleBadRequest(BadRequestException ex) {
        return this.configurateModelAndView(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ModelAndView handleForbidden(ForbiddenException ex) {
        return this.configurateModelAndView(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException ex) {
        return this.configurateModelAndView(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ModelAndView handleConflict(ConflictException ex) {
        return this.configurateModelAndView(HttpStatus.CONFLICT, ex.getMessage());
    }

    private ModelAndView configurateModelAndView(HttpStatusCode code, String message) {
        ModelAndView modelAndView = new ModelAndView(this.errorPageTemplate);
        modelAndView.setStatus(code);
        modelAndView.addObject("status", code.toString());
        modelAndView.addObject("message", message);
        return modelAndView;
    }
}
