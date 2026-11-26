package com.riftlog.exception;

/** The request is well-formed JSON but breaks a business rule (unknown card, wrong card type...). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
