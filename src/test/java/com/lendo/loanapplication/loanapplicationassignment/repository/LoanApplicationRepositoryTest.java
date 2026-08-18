package com.lendo.loanapplication.loanapplicationassignment.repository;

import com.lendo.loanapplication.loanapplicationassignment.AbstractPostgresTest;
import com.lendo.loanapplication.loanapplicationassignment.domain.Customer;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOffer;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOfferStatus;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplication;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LoanApplicationRepositoryTest extends AbstractPostgresTest {

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private LenderOfferRepository lenderOfferRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void findWithOffersByIdLoadsTheOffersEagerlyInOneQuery() {
        LoanApplication application = loanApplicationRepository.save(pendingApplication());
        lenderOfferRepository.save(offer(application, "Lender A"));
        lenderOfferRepository.save(offer(application, "Lender B"));
        entityManager.flush();
        entityManager.clear();

        LoanApplication found = loanApplicationRepository
                .findWithOffersById(application.getId())
                .orElseThrow();

        assertThat(Hibernate.isInitialized(found.getOffers()))
                .as("entity graph should have fetched the offers, not left a lazy proxy")
                .isTrue();
        assertThat(found.getOffers()).hasSize(2);
    }

    @Test
    void findWithOffersByIdReturnsEmptyForUnknownId() {
        assertThat(loanApplicationRepository.findWithOffersById(java.util.UUID.randomUUID()))
                .isEmpty();
    }

    @Test
    void findWithOffersByIdReturnsAnApplicationThatHasNoOffersYet() {
        LoanApplication application = loanApplicationRepository.save(pendingApplication());
        entityManager.flush();
        entityManager.clear();

        LoanApplication found = loanApplicationRepository
                .findWithOffersById(application.getId())
                .orElseThrow();

        assertThat(found.getOffers()).isEmpty();
    }

    @Test
    void rejectsASecondOfferFromTheSameLenderOnTheSameApplication() {
        LoanApplication application = loanApplicationRepository.save(pendingApplication());
        lenderOfferRepository.saveAndFlush(offer(application, "Lender A"));

        assertThatThrownBy(() -> lenderOfferRepository.saveAndFlush(offer(application, "Lender A")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsTheSameLenderToOfferOnDifferentApplications() {
        LoanApplication first = loanApplicationRepository.save(pendingApplication());
        LoanApplication second = loanApplicationRepository.save(pendingApplication());

        lenderOfferRepository.saveAndFlush(offer(first, "Lender A"));
        lenderOfferRepository.saveAndFlush(offer(second, "Lender A"));

        assertThat(lenderOfferRepository.findAll()).hasSize(2);
    }

    @Test
    void existsByApplicationIdAndLenderNameIsScopedToOneApplication() {
        LoanApplication application = loanApplicationRepository.save(pendingApplication());
        LoanApplication other = loanApplicationRepository.save(pendingApplication());
        lenderOfferRepository.saveAndFlush(offer(application, "Lender A"));

        assertThat(lenderOfferRepository.existsByApplicationIdAndLenderName(application.getId(), "Lender A"))
                .isTrue();
        assertThat(lenderOfferRepository.existsByApplicationIdAndLenderName(other.getId(), "Lender A"))
                .isFalse();
        assertThat(lenderOfferRepository.existsByApplicationIdAndLenderName(application.getId(), "Lender B"))
                .isFalse();
    }

    @Test
    void findByIdForUpdateReturnsTheApplication() {
        LoanApplication application = loanApplicationRepository.save(pendingApplication());
        entityManager.flush();

        Optional<LoanApplication> locked = loanApplicationRepository.findByIdForUpdate(application.getId());

        assertThat(locked).isPresent();
        assertThat(locked.orElseThrow().getId()).isEqualTo(application.getId());
    }

    @Test
    void acceptedAtIsNullUntilAcceptanceAndThenPersists() {
        LoanApplication application = loanApplicationRepository.saveAndFlush(pendingApplication());
        entityManager.clear();

        LoanApplication pending = loanApplicationRepository.findById(application.getId()).orElseThrow();
        assertThat(pending.getAcceptedAt()).isNull();

        Instant acceptedAt = Instant.parse("2026-08-17T19:20:00Z");
        pending.accept(acceptedAt);
        loanApplicationRepository.saveAndFlush(pending);
        entityManager.clear();

        LoanApplication accepted = loanApplicationRepository.findById(application.getId()).orElseThrow();
        assertThat(accepted.getStatus()).isEqualTo(LoanApplicationStatus.ACCEPTED);
        assertThat(accepted.getAcceptedAt()).isEqualTo(acceptedAt);
    }

    @Test
    void findAllWithSpecificationFiltersByStatus() {
        loanApplicationRepository.save(pendingApplication());
        LoanApplication accepted = loanApplicationRepository.save(application(LoanApplicationStatus.ACCEPTED));
        entityManager.flush();
        entityManager.clear();

        Specification<LoanApplication> isAccepted = (root, query, builder) ->
                builder.equal(root.get("status"), LoanApplicationStatus.ACCEPTED);

        var page = loanApplicationRepository.findAll(isAccepted, PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().getFirst().getId()).isEqualTo(accepted.getId());
    }

    private static LoanApplication pendingApplication() {
        return application(LoanApplicationStatus.PENDING);
    }

    private static LoanApplication application(LoanApplicationStatus status) {
        return LoanApplication.builder()
                .customer(Customer.builder()
                        .firstName("Customer")
                        .lastName("Surname")
                        .email("customer@example.com")
                        .build())
                .amount(new BigDecimal("50000.00"))
                .loanTerms(24)
                .status(status)
                .expiresAt(Instant.now().plus(Duration.ofDays(1)))
                .build();
    }

    private static LenderOffer offer(LoanApplication application, String lenderName) {
        return LenderOffer.builder()
                .application(application)
                .lenderName(lenderName)
                .annualInterestRate(new BigDecimal("5.90"))
                .monthlyPaymentAmount(new BigDecimal("1200.00"))
                .totalRepayment(new BigDecimal("43200.00"))
                .status(LenderOfferStatus.PENDING)
                .build();
    }
}
