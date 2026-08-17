package com.lendo.loanapplication.loanapplicationassignment.exception;

/**
 * Mapped once to 409 by GlobalExceptionHandler.
 */
public abstract class ConflictException extends RuntimeException {

    protected ConflictException(String message) {
        super(message);
    }
}
