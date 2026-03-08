package org.nelson.kidbank.controller;

import org.nelson.kidbank.exception.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        log.warn("Access denied for request {}: {}", request.getRequestURI(), e.getMessage());
        ModelAndView mav = new ModelAndView("error/4xx");
        mav.addObject("statusCode", 403);
        mav.addObject("message", "You do not have permission to perform this action.");
        return mav;
    }

    @ExceptionHandler(InsufficientFundsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleInsufficientFunds(InsufficientFundsException e, HttpServletRequest request) {
        return errorView(400, e.getMessage());
    }

    @ExceptionHandler(AccountClosedException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleAccountClosed(AccountClosedException e) {
        return errorView(400, e.getMessage());
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleAccountAlreadyExists(AccountAlreadyExistsException e) {
        return errorView(400, e.getMessage());
    }

    @ExceptionHandler(DuplicateUsernameException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleDuplicateUsername(DuplicateUsernameException e) {
        return errorView(400, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleIllegalArgument(IllegalArgumentException e) {
        return errorView(400, e.getMessage());
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoResource(HttpServletRequest request) {
        // Static resource misses (favicon.ico etc.) — log at debug, not error
        log.debug("No static resource found for {}", request.getRequestURI());
        return errorView(404, "Resource not found.");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGeneric(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception for request {}: {}", request.getRequestURI(), e.getMessage(), e);
        return errorView(500, "An unexpected error occurred. Please try again later.");
    }

    private ModelAndView errorView(int status, String message) {
        ModelAndView mav = new ModelAndView("error/generic");
        mav.addObject("statusCode", status);
        mav.addObject("message", message);
        return mav;
    }
}
