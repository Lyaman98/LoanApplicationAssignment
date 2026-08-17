package com.lendo.loanapplication.loanapplicationassignment.exception;


public abstract class NotFoundException extends RuntimeException {

    protected NotFoundException(String message) {
        super(message);
    }
}
