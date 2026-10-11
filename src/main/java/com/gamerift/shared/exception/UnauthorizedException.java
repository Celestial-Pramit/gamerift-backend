package com.gamerift.shared.exception;

import org.springframework.http.HttpStatus;

/** Thrown when login fails or a token is wrong: HTTP 401 (UNAUTHORIZED). */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
