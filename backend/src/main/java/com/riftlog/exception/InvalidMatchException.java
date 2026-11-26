package com.riftlog.exception;

/** A match payload that does not describe a valid Riftbound match (scores, game order, seats...). */
public class InvalidMatchException extends BadRequestException {

    public InvalidMatchException(String message) {
        super(message);
    }
}
