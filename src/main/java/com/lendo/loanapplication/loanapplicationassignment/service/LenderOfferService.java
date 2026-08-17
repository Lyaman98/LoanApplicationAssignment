package com.lendo.loanapplication.loanapplicationassignment.service;

import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferResponse;

import java.util.UUID;

public interface LenderOfferService {

    LenderOfferResponse submit(UUID applicationId, LenderOfferRequest request);
}
