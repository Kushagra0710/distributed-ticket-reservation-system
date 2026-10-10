package com.ticketing.ticket_reservation.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import com.ticketing.ticket_reservation.dto.ApiResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger LOGGER = Logger.getLogger(ApiExceptionHandler.class.getName());

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return ResponseEntity.badRequest()
            .body(new ApiResponse(false, "Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse> handleConstraintViolation(ConstraintViolationException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiResponse(false, "Validation failed", Map.of("request", exception.getMessage())));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiResponse(false, "Request body is missing or malformed", Map.of()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse> handleBadCredentials(BadCredentialsException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiResponse(false, exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiResponse(false, "You do not have permission to access this resource", Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleInvalidArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
            .body(new ApiResponse(false, exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleUploadTooLarge(MaxUploadSizeExceededException exception) {
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
            .body(new ApiResponse(false, "Poster image must be 5 MB or smaller.", Map.of("poster", "File is too large.")));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiResponse> handleExternalServiceUnavailable(ResourceAccessException exception) {
        LOGGER.log(Level.WARNING, "Postal pincode lookup service is unavailable", exception);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .body(new ApiResponse(false, "Pincode lookup is temporarily unavailable. Please try again.", Map.of()));
    }

    @ExceptionHandler(EmailAlreadyRegisteredException.class)
    public ResponseEntity<ApiResponse> handleDuplicateEmail(EmailAlreadyRegisteredException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiResponse(false, exception.getMessage(), Map.of("email", exception.getMessage())));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse> handleDataConflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiResponse(false, "The request conflicts with existing data", Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleUnexpected(Exception exception) {
        LOGGER.log(Level.SEVERE, "Unexpected API error", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiResponse(false, "An unexpected error occurred", Map.of(
                "type", exception.getClass().getSimpleName())));
    }
}