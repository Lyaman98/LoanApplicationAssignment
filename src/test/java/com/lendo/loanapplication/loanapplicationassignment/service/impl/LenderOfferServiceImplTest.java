package com.lendo.loanapplication.loanapplicationassignment.service.impl;

import com.lendo.loanapplication.loanapplicationassignment.domain.Customer;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LenderOfferServiceImplTest {

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @Mock
    private LenderOfferRepository lenderOfferRepository;

    @InjectMocks
    private LenderOfferServiceImpl service;

    @Test
    void savesOfferAsPendingAndReturnsIt() {
        UUID applicationId = UUID.randomUUID();
        when(loanApplicationRepository.findByIdForUpdate(applicationId))
                .thenReturn(Optional.of(pendingApplication(applicationId)));
        when(lenderOfferRepository.existsByApplicationIdAndLenderName(applicationId, "Lender A"))
                .thenReturn(false);
        when(lenderOfferRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LenderOfferResponse response = service.submit(applicationId, request("Lender A"));

        assertThat(response.status()).isEqualTo(LenderOfferStatus.PENDING);
        assertThat(response.lenderName()).isEqualTo("Lender A");
        assertThat(response.annualInterestRate()).isEqualByComparingTo("5.90");
    }

    @Test
    void trimsLenderNameBeforeCheckingAndSaving() {
        UUID applicationId = UUID.randomUUID();
        when(loanApplicationRepository.findByIdForUpdate(applicationId))
                .thenReturn(Optional.of(pendingApplication(applicationId)));
        when(lenderOfferRepository.existsByApplicationIdAndLenderName(applicationId, "Lender A"))
                .thenReturn(false);
        when(lenderOfferRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LenderOfferResponse response = service.submit(applicationId, request("  Lender A  "));

        assertThat(response.lenderName()).isEqualTo("Lender A");
        verify(lenderOfferRepository).existsByApplicationIdAndLenderName(applicationId, "Lender A");
    }

    @Test
    void throwsWhenApplicationDoesNotExist() {
        UUID applicationId = UUID.randomUUID();
        when(loanApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.submit(applicationId, request("Lender A")))
                .isInstanceOf(LoanApplicationNotFoundException.class);

        verify(lenderOfferRepository, never()).save(any());
    }

    @Test
    void throwsWhenApplicationIsAlreadyAccepted() {
        UUID applicationId = UUID.randomUUID();
        LoanApplication application = application(applicationId, LoanApplicationStatus.ACCEPTED, notYetExpired());
        when(loanApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.submit(applicationId, request("Lender A")))
                .isInstanceOf(ApplicationNotOpenException.class)
                .hasMessageContaining("ACCEPTED");

        verify(lenderOfferRepository, never()).save(any());
    }

    @Test
    void expiresApplicationBeforeSubmittingAnOffer() {
        UUID applicationId = UUID.randomUUID();
        LoanApplication application =
                application(applicationId, LoanApplicationStatus.PENDING, Instant.now().minusSeconds(1));
        when(loanApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.submit(applicationId, request("Lender A")))
                .isInstanceOf(ApplicationNotOpenException.class)
                .hasMessageContaining("EXPIRED");

        assertThat(application.getStatus()).isEqualTo(LoanApplicationStatus.EXPIRED);
        assertThat(application.getExpiredAt()).isNotNull();
        verify(lenderOfferRepository, never()).save(any());
    }

    /** Status is checked before duplication, so a closed application says so rather than "duplicate". */
    @Test
    void reportsClosedApplicationRatherThanDuplicateWhenBothApply() {
        UUID applicationId = UUID.randomUUID();
        LoanApplication application = application(applicationId, LoanApplicationStatus.ACCEPTED, notYetExpired());
        when(loanApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));

        assertThatThrownBy(() -> service.submit(applicationId, request("Lender A")))
                .isInstanceOf(ApplicationNotOpenException.class);

        verify(lenderOfferRepository, never()).existsByApplicationIdAndLenderName(any(), any());
    }

    @Test
    void throwsWhenLenderAlreadySubmittedAnOffer() {
        UUID applicationId = UUID.randomUUID();
        when(loanApplicationRepository.findByIdForUpdate(applicationId))
                .thenReturn(Optional.of(pendingApplication(applicationId)));
        when(lenderOfferRepository.existsByApplicationIdAndLenderName(applicationId, "Lender A"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.submit(applicationId, request("Lender A")))
                .isInstanceOf(DuplicateOfferException.class)
                .hasMessageContaining("Lender A");

        verify(lenderOfferRepository, never()).save(any());
    }

    @Test
    void linksTheOfferToItsApplication() {
        UUID applicationId = UUID.randomUUID();
        LoanApplication application = pendingApplication(applicationId);
        when(loanApplicationRepository.findByIdForUpdate(applicationId)).thenReturn(Optional.of(application));
        when(lenderOfferRepository.existsByApplicationIdAndLenderName(eq(applicationId), any()))
                .thenReturn(false);
        when(lenderOfferRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.submit(applicationId, request("Lender A"));

        verify(lenderOfferRepository)
                .save(org.mockito.ArgumentMatchers.argThat(offer -> offer.getApplication() == application));
    }

    private static LenderOfferRequest request(String lenderName) {
        return new LenderOfferRequest(
                lenderName, new BigDecimal("5.90"), new BigDecimal("1200.00"), new BigDecimal("43200.00"));
    }

    private static Instant notYetExpired() {
        return Instant.now().plus(Duration.ofDays(1));
    }

    private static LoanApplication pendingApplication(UUID id) {
        return application(id, LoanApplicationStatus.PENDING, notYetExpired());
    }

    private static LoanApplication application(UUID id, LoanApplicationStatus status, Instant expiresAt) {
        return LoanApplication.builder()
                .id(id)
                .customer(Customer.builder()
                        .firstName("Jane")
                        .lastName("Doe")
                        .email("jane.doe@example.com")
                        .build())
                .amount(new BigDecimal("50000.00"))
                .loanTerms(24)
                .status(status)
                .createdAt(Instant.parse("2026-01-15T10:00:00Z"))
                .expiresAt(expiresAt)
                .offers(new ArrayList<>())
                .build();
    }
}
