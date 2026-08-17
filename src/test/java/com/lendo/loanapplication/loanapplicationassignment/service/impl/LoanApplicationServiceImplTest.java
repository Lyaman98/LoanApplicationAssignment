package com.lendo.loanapplication.loanapplicationassignment.service.impl;


import com.lendo.loanapplication.loanapplicationassignment.config.LoanPolicyProperties;
import com.lendo.loanapplication.loanapplicationassignment.domain.*;
import com.lendo.loanapplication.loanapplicationassignment.dto.CreateLoanApplicationRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationResponse;
import com.lendo.loanapplication.loanapplicationassignment.exception.*;
import com.lendo.loanapplication.loanapplicationassignment.repository.LoanApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanApplicationServiceImplTest {

    private static final BigDecimal MIN_AMOUNT = new BigDecimal("1000.00");
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("5000000.00");
    private static final int MIN_TERMS = 1;
    private static final int MAX_TERMS = 360;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    private LoanApplicationServiceImpl service;

    @BeforeEach
    void setUp() {
        LoanPolicyProperties policy = new LoanPolicyProperties(MIN_AMOUNT, MAX_AMOUNT, MIN_TERMS, MAX_TERMS);
        service = new LoanApplicationServiceImpl(loanApplicationRepository, policy);
    }

    @Nested
    class Create {

        @Test
        void savesApplicationAsPendingAndReturnsIt() {
            when(loanApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            LoanApplicationResponse response = service.create(request(new BigDecimal("50000.00"), 24));

            assertThat(response.status()).isEqualTo(LoanApplicationStatus.PENDING);
            assertThat(response.amount()).isEqualByComparingTo("50000.00");
            assertThat(response.loanTerms()).isEqualTo(24);
            assertThat(response.customer().email()).isEqualTo("customer@example.com");
            assertThat(response.offers()).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"1000.00", "5000000.00", "50000.00"})
        void acceptsAmountsInsidePolicy(String amount) {
            when(loanApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            assertThat(service.create(request(new BigDecimal(amount), 24))).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"999.99", "5000000.01"})
        void rejectsAmountsOutsidePolicy(String amount) {
            assertThatThrownBy(() -> service.create(request(new BigDecimal(amount), 24)))
                    .isInstanceOf(LoanPolicyViolationException.class)
                    .hasMessageContaining("amount");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 360})
        void acceptsTermsInsidePolicy(int loanTerms) {
            when(loanApplicationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            assertThat(service.create(request(new BigDecimal("50000.00"), loanTerms))).isNotNull();
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 361})
        void rejectsTermsOutsidePolicy(int loanTerms) {
            assertThatThrownBy(() -> service.create(request(new BigDecimal("50000.00"), loanTerms)))
                    .isInstanceOf(LoanPolicyViolationException.class)
                    .hasMessageContaining("term");
        }

        @Test
        void doesNotPersistAnythingWhenPolicyIsViolated() {
            assertThatThrownBy(() -> service.create(request(new BigDecimal("10.00"), 24)))
                    .isInstanceOf(LoanPolicyViolationException.class);

            verify(loanApplicationRepository, never()).save(any());
        }

        private CreateLoanApplicationRequest request(BigDecimal amount, int loanTerms) {
            return new CreateLoanApplicationRequest("Customer Name", "Customer LastName", "customer@example.com", amount, loanTerms);
        }
    }

    @Nested
    class GetById {

        @Test
        void returnsApplicationWithOffers() {
            UUID id = UUID.randomUUID();
            LoanApplication application = pendingApplication(id);
            application.getOffers().add(offer(UUID.randomUUID(), application, "Lender A", LenderOfferStatus.PENDING));
            when(loanApplicationRepository.findWithOffersById(id)).thenReturn(Optional.of(application));

            LoanApplicationResponse response = service.getById(id);

            assertThat(response.id()).isEqualTo(id);
            assertThat(response.offers()).hasSize(1);
        }

        @Test
        void throwsWhenApplicationDoesNotExist() {
            UUID id = UUID.randomUUID();
            when(loanApplicationRepository.findWithOffersById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getById(id)).isInstanceOf(LoanApplicationNotFoundException.class);
        }
    }

    @Nested
    class GetAll {

        @Test
        void mapsPagedResultsAndKeepsPaginationMetadata() {
            when(loanApplicationRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(
                            org.springframework.data.domain.Pageable.class)))
                    .thenReturn(new PageImpl<>(
                            List.of(pendingApplication(UUID.randomUUID())), PageRequest.of(0, 20), 1));

            var page = service.getAll(null, null, null, PageRequest.of(0, 20));

            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getTotalElements()).isEqualTo(1);
        }

        @Test
        void rejectsReversedDateRangeWithoutQueryingTheDatabase() {
            Instant from = Instant.parse("2026-02-01T00:00:00Z");
            Instant to = Instant.parse("2026-01-01T00:00:00Z");

            assertThatThrownBy(() -> service.getAll(null, from, to, PageRequest.of(0, 20)))
                    .isInstanceOf(InvalidFilterException.class)
                    .hasMessageContaining("createdFrom");

            verify(loanApplicationRepository, never())
                    .findAll(any(org.springframework.data.jpa.domain.Specification.class), any(
                            org.springframework.data.domain.Pageable.class));
        }
    }

    private static LoanApplication pendingApplication(UUID id) {
        return LoanApplication.builder()
                .id(id)
                .customer(Customer.builder()
                        .firstName("Customer")
                        .lastName("LastName")
                        .email("customer@example.com")
                        .build())
                .amount(new BigDecimal("50000.00"))
                .loanTerms(24)
                .status(LoanApplicationStatus.PENDING)
                .createdAt(Instant.parse("2026-01-15T10:00:00Z"))
                .offers(new ArrayList<>())
                .build();
    }

    private static LenderOffer offer(
            UUID id, LoanApplication application, String lenderName, LenderOfferStatus status) {
        return LenderOffer.builder()
                .id(id)
                .application(application)
                .lenderName(lenderName)
                .annualInterestRate(new BigDecimal("5.90"))
                .monthlyPaymentAmount(new BigDecimal("1200.00"))
                .totalRepayment(new BigDecimal("43200.00"))
                .status(status)
                .createdAt(Instant.parse("2026-01-15T11:00:00Z"))
                .build();
    }
}
