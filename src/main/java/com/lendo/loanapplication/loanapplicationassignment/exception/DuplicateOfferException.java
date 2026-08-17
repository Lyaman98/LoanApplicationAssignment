package com.lendo.loanapplication.loanapplicationassignment.exception;

import java.util.UUID;

/** Each lender may submit at most one offer per application. */
public class DuplicateOfferException extends ConflictException {

    public DuplicateOfferException(UUID applicationId, String lenderName) {
        super("Lender %s has already submitted an offer on application %s".formatted(lenderName, applicationId));
    }
}
