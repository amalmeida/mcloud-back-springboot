package com.mcloud.auth0_authenticator.domain.exception;

import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Locale;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundException ex, Locale locale) {
        String message = messageSource.getMessage("error.user.not.found", new Object[]{ex.getUserId()}, locale);
        return new ResponseEntity<>(new ErrorResponse(ErrorCode.USER_NOT_FOUND, message), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Auth0Exception.class)
    public ResponseEntity<ErrorResponse> handleAuth0Exception(Auth0Exception ex, Locale locale) {
        if (ex instanceof APIException apiEx) {
            if (apiEx.getStatusCode() == 404) {
                String message = messageSource.getMessage("error.auth0.user.not.found", new Object[]{apiEx.getDescription()}, locale);
                return new ResponseEntity<>(new ErrorResponse(ErrorCode.AUTH0_USER_NOT_FOUND, message), HttpStatus.NOT_FOUND);
            } else if (apiEx.getStatusCode() == 401 || apiEx.getStatusCode() == 403) {
                String message = messageSource.getMessage("error.auth0.unauthorized", null, locale);
                return new ResponseEntity<>(new ErrorResponse(ErrorCode.AUTH0_UNAUTHORIZED, message), HttpStatus.UNAUTHORIZED);
            }
        }
        String message = messageSource.getMessage("error.auth0.sync.failed", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(ErrorCode.AUTH0_UPDATE_FAILED, message), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, Locale locale) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> messageSource.getMessage(error.getDefaultMessage(), null, locale))
                .collect(Collectors.joining("; "));
        return new ResponseEntity<>(new ErrorResponse(ErrorCode.VALIDATION_ERROR, message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, Locale locale) {
        String message = messageSource.getMessage("error.invalid.json", null, locale);
        return new ResponseEntity<>(new ErrorResponse(ErrorCode.INVALID_JSON, message), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, Locale locale) {
        String message = messageSource.getMessage("error.generic", new Object[]{ex.getMessage()}, locale);
        return new ResponseEntity<>(new ErrorResponse(ErrorCode.GENERIC_ERROR, message), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}