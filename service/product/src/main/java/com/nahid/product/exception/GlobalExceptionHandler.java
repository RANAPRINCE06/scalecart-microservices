package com.nahid.product.exception;

import com.nahid.product.dto.response.ApiResponse;
import com.nahid.product.util.helper.ApiResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;

import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        return ApiResponseUtil.failureWithHttpStatus(ex.getMessage(), HttpStatus.NOT_FOUND);
    }


    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiResponse<Object>> handleDuplicateResourceException(DuplicateResourceException ex) {
        return ApiResponseUtil.failureWithHttpStatus(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(PurchaseException.class)
    public ResponseEntity<ApiResponse<Object>> handlePurchaseException(PurchaseException ex) {
        return ApiResponseUtil.failureWithHttpStatus(ex.getMessage(), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String key = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(key, errorMessage);
        });

        return ApiResponseUtil.failureWithData(errors, "Invalid input data", HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(StockUpdateException.class)
    public ResponseEntity<ApiResponse<Object>> handleStockUpdateException(StockUpdateException ex) {
        return ApiResponseUtil.failureWithHttpStatus(ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        return ApiResponseUtil.failure("An unexpected error occurred");
    }
}
