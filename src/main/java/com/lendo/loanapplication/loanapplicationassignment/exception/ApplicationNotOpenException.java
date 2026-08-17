package com.lendo.loanapplication.loanapplicationassignment.exception;

import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;

import java.util.UUID;

public class ApplicationNotOpenException extends ConflictException {

    public ApplicationNotOpenException(UUID applicationId, LoanApplicationStatus status) {
        super("Loan application %s is %s and no longer open".formatted(applicationId, status));
    }
}
