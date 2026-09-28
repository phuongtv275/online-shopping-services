package com.example.promotionservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> implements Serializable {

    private int status;
    private String message;
    private T data;
    private String correlationId;
    private Instant timestamp;

    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return ApiResponse.<T>builder()
                .status(200)
                .message(message)
                .data(data)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> created(T data, String message, String correlationId) {
        return ApiResponse.<T>builder()
                .status(201)
                .message(message)
                .data(data)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message, String correlationId) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .correlationId(correlationId)
                .timestamp(Instant.now())
                .build();
    }

    public static <T> ApiResponse<T> error(int status, String message) {
        return error(status, message, org.slf4j.MDC.get("correlationId"));
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return success(data, message, org.slf4j.MDC.get("correlationId"));
    }

    public static <T> ApiResponse<T> created(T data, String message) {
        return created(data, message, org.slf4j.MDC.get("correlationId"));
    }
}
