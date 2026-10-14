package com.riftlog.exception;

public class DeckInUseException extends RuntimeException {

    public DeckInUseException(String message) {
        super(message);
    }
}
