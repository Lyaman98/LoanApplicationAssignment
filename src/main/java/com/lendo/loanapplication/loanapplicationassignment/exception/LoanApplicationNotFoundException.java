package com.lendo.loanapplication.loanapplicationassignment.exception;

import java.util.UUID;

public class LoanApplicationNotFoundException extends NotFoundException {

    public LoanApplicationNotFoundException(UUID id) {
        super("Loan application %s not found".formatted(id));
    }
}
