package com.lendo.loanapplication.loanapplicationassignment.exception;

import java.util.UUID;

public class LenderOfferNotFoundException extends NotFoundException {

    public LenderOfferNotFoundException(UUID applicationId, UUID offerId) {
        super("Offer %s not found on loan application %s".formatted(offerId, applicationId));
    }
}
