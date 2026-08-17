package com.lendo.loanapplication.loanapplicationassignment.dto;

import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplication;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


public record LoanApplicationSummaryResponse(
        UUID id,
        CustomerResponse customer,
        BigDecimal amount,
        int loanTerms,
        LoanApplicationStatus status,
        Instant createdAt) {

    public static LoanApplicationSummaryResponse from(LoanApplication application) {
        return new LoanApplicationSummaryResponse(
                application.getId(),
                CustomerResponse.from(application.getCustomer()),
                application.getAmount(),
                application.getLoanTerms(),
                application.getStatus(),
                application.getCreatedAt());
    }
}
