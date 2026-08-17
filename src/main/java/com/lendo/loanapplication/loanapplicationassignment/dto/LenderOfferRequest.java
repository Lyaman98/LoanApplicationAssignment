package com.lendo.loanapplication.loanapplicationassignment.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record LenderOfferRequest(
        @NotBlank String lenderName,
        @NotNull @DecimalMin(value = "0.00") @DecimalMax(value = "100.00") BigDecimal annualInterestRate,
        @NotNull @DecimalMin(value = "0.01") BigDecimal monthlyPaymentAmount,
        @NotNull @DecimalMin(value = "0.01") BigDecimal totalRepayment) {}
