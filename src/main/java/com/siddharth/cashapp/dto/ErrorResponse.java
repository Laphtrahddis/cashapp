package com.siddharth.cashapp.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * The JSON body for every error the API returns.
 * fieldErrors is only filled for validation errors, e.g. {"totalAmount": "totalAmount is required"}.
 */
public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
}
