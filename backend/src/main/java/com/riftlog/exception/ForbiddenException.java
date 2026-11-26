package com.riftlog.exception;

/** The user is authenticated but may not change this resource. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
