package com.gamerift.shared.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the data clashes with existing data (for example duplicate email): HTTP 409 (CONFLICT). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
