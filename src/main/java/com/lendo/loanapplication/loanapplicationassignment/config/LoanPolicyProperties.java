package com.lendo.loanapplication.loanapplicationassignment.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;


@Validated
@ConfigurationProperties(prefix = "lendo.loan-policy")
public record LoanPolicyProperties(
        @NotNull @Positive BigDecimal minAmount,
        @NotNull @Positive BigDecimal maxAmount,
        @Positive int minLoanTerms,
        @Positive int maxLoanTerms) {}
