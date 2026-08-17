package com.lendo.loanapplication.loanapplicationassignment.service;

import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import com.lendo.loanapplication.loanapplicationassignment.dto.CreateLoanApplicationRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationResponse;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface LoanApplicationService {

    LoanApplicationResponse create(CreateLoanApplicationRequest request);

    LoanApplicationResponse getById(UUID id);

    Page<LoanApplicationSummaryResponse> getAll(
            LoanApplicationStatus status, Instant createdFrom, Instant createdTo, Pageable pageable);

    LoanApplicationResponse acceptLenderOffer(UUID applicationId, UUID offerId);
}
