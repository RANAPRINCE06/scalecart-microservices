package com.nahid.product.util.helper;

import com.nahid.product.dto.response.ApiResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiResponseUtil {

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message) {
        return success(data, message, HttpStatus.OK);
    }

    public static <T> ResponseEntity<ApiResponse<T>> success(T data, String message, HttpStatus httpStatus) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .statusCode(httpStatus.value())
                .build();
        return ResponseEntity.status(httpStatus).body(response);
    }

    public static <T> ResponseEntity<ApiResponse<T>> failure(String message) {
        return failureWithHttpStatus(null, message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static <T> ResponseEntity<ApiResponse<T>> failureWithHttpStatus(String message, HttpStatus httpStatus) {
        return failureWithHttpStatus(null, message, httpStatus);
    }

    public static <T> ResponseEntity<ApiResponse<T>> failureWithData(T data, String message, HttpStatus httpStatus) {
        return failureWithHttpStatus(data, message, httpStatus);
    }

    private static <T> ResponseEntity<ApiResponse<T>> failureWithHttpStatus(T data, String message, HttpStatus httpStatus) {
        ApiResponse<T> response = ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .timestamp(Instant.now())
                .statusCode(httpStatus.value())
                .build();
        return ResponseEntity.status(httpStatus).body(response);
    }
}
