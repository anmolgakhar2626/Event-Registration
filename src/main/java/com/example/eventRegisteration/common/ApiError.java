package com.example.eventRegisteration.common;

import java.time.LocalDateTime;

/**
 * Standard API error envelope returned on all error responses.
 */
public record ApiError(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, LocalDateTime.now());
    }
}
