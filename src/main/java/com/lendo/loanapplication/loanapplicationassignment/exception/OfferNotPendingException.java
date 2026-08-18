package com.lendo.loanapplication.loanapplicationassignment.exception;

import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOfferStatus;

import java.util.UUID;

/**
 * The offer exists but has already been decided, so it cannot be accepted.
 *
 * <p>Distinct from {@link LenderOfferNotFoundException}: "no such offer" is a 404, "that offer is not
 * in an acceptable state" is a 409.
 */
public class OfferNotPendingException extends ConflictException {

    public OfferNotPendingException(UUID applicationId, UUID offerId, LenderOfferStatus status) {
        super("Offer %s on application %s is %s and can no longer be accepted"
                .formatted(offerId, applicationId, status));
    }
}
