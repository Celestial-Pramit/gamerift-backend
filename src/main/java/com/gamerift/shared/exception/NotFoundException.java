package com.gamerift.shared.exception;

import org.springframework.http.HttpStatus;

/** Thrown when something does not exist: HTTP 404 (NOT_FOUND). */
public class NotFoundException extends ApiException {

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
