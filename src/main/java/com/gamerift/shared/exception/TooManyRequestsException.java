package com.gamerift.shared.exception;

import org.springframework.http.HttpStatus;

/** Thrown when someone asks too often (rate limit): HTTP 429 (TOO_MANY_REQUESTS). */
public class TooManyRequestsException extends ApiException {

    public TooManyRequestsException(String message) {
        super(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}
