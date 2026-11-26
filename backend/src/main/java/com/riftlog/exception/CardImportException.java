package com.riftlog.exception;

/** Riftcodex could not be reached or returned something unusable. */
public class CardImportException extends RuntimeException {

    public CardImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
