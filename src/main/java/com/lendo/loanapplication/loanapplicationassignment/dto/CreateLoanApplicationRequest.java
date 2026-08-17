package com.lendo.loanapplication.loanapplicationassignment.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateLoanApplicationRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotNull @Positive @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Positive int loanTerms) {}
