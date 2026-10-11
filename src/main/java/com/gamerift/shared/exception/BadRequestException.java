package com.gamerift.shared.exception;

import org.springframework.http.HttpStatus;

/** Thrown when the request is wrong (bad data, broken rule): HTTP 400 (BAD_REQUEST). */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
