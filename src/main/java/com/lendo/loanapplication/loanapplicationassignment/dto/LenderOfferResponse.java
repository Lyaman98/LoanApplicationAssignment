package com.lendo.loanapplication.loanapplicationassignment.dto;

import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOffer;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOfferStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LenderOfferResponse(
        UUID id,
        String lenderName,
        BigDecimal annualInterestRate,
        BigDecimal monthlyPaymentAmount,
        BigDecimal totalRepayment,
        LenderOfferStatus status,
        Instant createdAt) {

    public static LenderOfferResponse from(LenderOffer offer) {
        return new LenderOfferResponse(
                offer.getId(),
                offer.getLenderName(),
                offer.getAnnualInterestRate(),
                offer.getMonthlyPaymentAmount(),
                offer.getTotalRepayment(),
                offer.getStatus(),
                offer.getCreatedAt());
    }
}
