package com.gamerift.shared.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class of our own "expected" errors (not found, bad request, conflict...).
 * Services throw these; GlobalExceptionHandler turns them into a clean JSON response.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
