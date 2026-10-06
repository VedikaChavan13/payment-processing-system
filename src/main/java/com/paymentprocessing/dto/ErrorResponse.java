package com.paymentprocessing.dto;

import java.time.Instant;
import java.util.Map;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse validation(int status, String message, String path, Map<String, String> validationErrors) {
        return new ErrorResponse(Instant.now(), status, "VALIDATION_FAILED", message, path, validationErrors);
    }
}
