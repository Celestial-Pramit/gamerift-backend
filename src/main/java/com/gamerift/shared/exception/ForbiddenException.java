package com.gamerift.shared.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the user is known but not allowed (for example a banned account): HTTP 403 (FORBIDDEN). */
public class ForbiddenException extends ApiException {

    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
