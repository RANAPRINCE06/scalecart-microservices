package com.nahid.userservice.exception;

import com.nahid.userservice.dto.response.ApiResponse;
import com.nahid.userservice.util.helper.ApiResponseUtil;
import com.nahid.userservice.util.constant.ExceptionMessageConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.nio.file.AccessDeniedException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationErrors(
            MethodArgumentNotValidException ex
    ) {
        log.debug("Validation error: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            fieldErrors.put(fieldName, errorMessage);
        });

        return ApiResponseUtil.failureWithHttpStatus(
                ExceptionMessageConstant.VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                fieldErrors
        );
    }


    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex

    ) {
        log.debug("Authentication error: {}", ex.getMessage());
        return ApiResponseUtil.failureWithHttpStatus(ex.getMessage(), HttpStatus.UNAUTHORIZED);
    }


    @ExceptionHandler({BadCredentialsException.class})
    public ResponseEntity<ApiResponse<Object>> handleSpringAuthenticationException(
            Exception ex,
            WebRequest request
    ) {
        log.debug("Spring Security authentication error: {}", ex.getMessage());
        return ApiResponseUtil.failureWithHttpStatus(ExceptionMessageConstant.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request
    ) {
        log.debug("Access denied: {}", ex.getMessage());
        return ApiResponseUtil.failureWithHttpStatus(ExceptionMessageConstant.ACCESS_DENIED, HttpStatus.FORBIDDEN);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex,
            WebRequest request
    ) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ApiResponseUtil.failureWithHttpStatus(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(
            Exception ex,
            WebRequest request

    ) {
        //log.error("Unexpected error: {}", ex.getMessage(), ex);
        return ApiResponseUtil.failureWithHttpStatus(
                ExceptionMessageConstant.UNEXPECTED_ERROR,
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}