package com.gamerift.shared.exception;

import java.time.Instant;
import java.util.Map;

import lombok.Getter;

/**
 * The JSON body we return for every error, so the frontend always sees the same shape:
 * { timestamp, status, error, message, path, fieldErrors }.
 */
@Getter
public class ApiError {

    private final Instant timestamp = Instant.now();
    private final int status;
    private final String error;
    private final String message;
    private final String path;
    // Only filled for validation errors: field name -> message
    private final Map<String, String> fieldErrors;

    public ApiError(int status, String error, String message, String path, Map<String, String> fieldErrors) {
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
        this.fieldErrors = fieldErrors;
    }
}
