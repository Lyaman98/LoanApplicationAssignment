package com.lendo.loanapplication.loanapplicationassignment.dto;


import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplication;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;


public record LoanApplicationResponse(
        UUID id,
        CustomerResponse customer,
        BigDecimal amount,
        int loanTerms,
        LoanApplicationStatus status,
        Instant createdAt,
        List<LenderOfferResponse> offers) {

    public static LoanApplicationResponse from(LoanApplication application) {
        return new LoanApplicationResponse(
                application.getId(),
                CustomerResponse.from(application.getCustomer()),
                application.getAmount(),
                application.getLoanTerms(),
                application.getStatus(),
                application.getCreatedAt(),
                application.getOffers().stream().map(LenderOfferResponse::from).toList());
    }
}
