package com.foodya.backend.interfaces.rest;

import com.foodya.backend.interfaces.rest.dto.ApiErrorResponse;
import com.foodya.backend.application.exception.ConflictException;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.TooManyRequestsException;
import com.foodya.backend.application.exception.UnauthorizedException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.domain.exceptions.CartValidationException;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), null, request);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), null, request);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), null, request);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(ValidationException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", ex.getMessage(), ex.getDetails(), request);
    }

    @ExceptionHandler(CartValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleCartValidation(CartValidationException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", ex.getMessage(), null, request);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null, request);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<ApiErrorResponse> handleTooManyRequests(TooManyRequestsException ex, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ex.getMessage(), null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", "Request validation failed", details, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        return error(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_FAILED", ex.getMessage(), null, request);
    }

    @ExceptionHandler({
            org.springframework.web.multipart.MultipartException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.HttpMediaTypeNotSupportedException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(Exception ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error", null, request);
    }

    private ResponseEntity<ApiErrorResponse> error(HttpStatusCode status,
                                                   String code,
                                                   String message,
                                                   Object details,
                                                   HttpServletRequest request) {
        ApiErrorResponse response = new ApiErrorResponse(code, message, details, RequestTrace.from(request));
        return ResponseEntity.status(Objects.requireNonNull(status)).body(response);
    }
}
