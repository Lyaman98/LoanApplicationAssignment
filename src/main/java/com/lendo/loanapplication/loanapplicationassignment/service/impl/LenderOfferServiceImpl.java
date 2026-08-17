package com.lendo.loanapplication.loanapplicationassignment.service.impl;


import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOffer;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOfferStatus;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplication;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferResponse;
import com.lendo.loanapplication.loanapplicationassignment.exception.ApplicationNotOpenException;
import com.lendo.loanapplication.loanapplicationassignment.exception.DuplicateOfferException;
import com.lendo.loanapplication.loanapplicationassignment.exception.LoanApplicationNotFoundException;
import com.lendo.loanapplication.loanapplicationassignment.repository.LenderOfferRepository;
import com.lendo.loanapplication.loanapplicationassignment.repository.LoanApplicationRepository;
import com.lendo.loanapplication.loanapplicationassignment.service.LenderOfferService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LenderOfferServiceImpl implements LenderOfferService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LenderOfferRepository lenderOfferRepository;

    @Override
    @Transactional
    public LenderOfferResponse submit(UUID applicationId, LenderOfferRequest request) {
        LoanApplication application = loanApplicationRepository
                .findByIdForUpdate(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException(applicationId));

        if (application.getStatus() != LoanApplicationStatus.PENDING) {
            throw new ApplicationNotOpenException(applicationId, application.getStatus());
        }

        String lenderName = request.lenderName().trim();
        if (lenderOfferRepository.existsByApplicationIdAndLenderName(applicationId, lenderName)) {
            throw new DuplicateOfferException(applicationId, lenderName);
        }

        LenderOffer offer = lenderOfferRepository.save(LenderOffer.builder()
                .application(application)
                .lenderName(lenderName)
                .annualInterestRate(request.annualInterestRate())
                .monthlyPaymentAmount(request.monthlyPaymentAmount())
                .totalRepayment(request.totalRepayment())
                .status(LenderOfferStatus.PENDING)
                .build());

        log.info("Offer submitted: applicationId={}, offerId={}, lenderName={}", applicationId, offer.getId(), lenderName);

        return LenderOfferResponse.from(offer);
    }
}
