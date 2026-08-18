package com.lendo.loanapplication.loanapplicationassignment.concurrency;

import com.lendo.loanapplication.loanapplicationassignment.AbstractPostgresTest;
import com.lendo.loanapplication.loanapplicationassignment.domain.Customer;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOffer;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOfferStatus;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplication;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferRequest;
import com.lendo.loanapplication.loanapplicationassignment.exception.ApplicationNotOpenException;
import com.lendo.loanapplication.loanapplicationassignment.exception.DuplicateOfferException;
import com.lendo.loanapplication.loanapplicationassignment.repository.LenderOfferRepository;
import com.lendo.loanapplication.loanapplicationassignment.repository.LoanApplicationRepository;
import com.lendo.loanapplication.loanapplicationassignment.service.LenderOfferService;
import com.lendo.loanapplication.loanapplicationassignment.service.LoanApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
class OfferAcceptanceConcurrencyTest extends AbstractPostgresTest {

    @Autowired
    private LoanApplicationService loanApplicationService;

    @Autowired
    private LenderOfferService lenderOfferService;

    @Autowired
    private LoanApplicationRepository loanApplicationRepository;

    @Autowired
    private LenderOfferRepository lenderOfferRepository;

    @AfterEach
    void cleanUp() {
        lenderOfferRepository.deleteAll();
        loanApplicationRepository.deleteAll();
    }

    @Test
    void twoCustomersAcceptingDifferentOffersAtOnceProduceExactlyOneAcceptance() throws Exception {
        LoanApplication application = loanApplicationRepository.save(pendingApplication());
        UUID applicationId = application.getId();
        UUID offerA = lenderOfferRepository.save(offer(application, "Lender A")).getId();
        UUID offerB = lenderOfferRepository.save(offer(application, "Lender B")).getId();

        List<Throwable> failures = runConcurrently(
                () -> loanApplicationService.acceptLenderOffer(applicationId, offerA),
                () -> loanApplicationService.acceptLenderOffer(applicationId, offerB));

        assertThat(failures)
                .as("exactly one thread should lose the race")
                .hasSize(1);
        assertThat(failures.getFirst()).isInstanceOf(ApplicationNotOpenException.class);

        LoanApplication reloaded =
                loanApplicationRepository.findWithOffersById(applicationId).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(LoanApplicationStatus.ACCEPTED);
        assertThat(reloaded.getOffers())
                .filteredOn(offer -> offer.getStatus() == LenderOfferStatus.ACCEPTED)
                .hasSize(1);
        assertThat(reloaded.getOffers())
                .filteredOn(offer -> offer.getStatus() == LenderOfferStatus.REJECTED)
                .hasSize(1);
    }

    @Test
    void theSameLenderSubmittingTwiceAtOnceGetsExactlyOneOffer() throws Exception {
        LoanApplication application = loanApplicationRepository.save(pendingApplication());
        UUID applicationId = application.getId();

        List<Throwable> failures = runConcurrently(
                () -> lenderOfferService.submit(applicationId, offerRequest()),
                () -> lenderOfferService.submit(applicationId, offerRequest()));

        assertThat(failures).hasSize(1);
        assertThat(failures.getFirst()).isInstanceOf(DuplicateOfferException.class);
        assertThat(lenderOfferRepository.findAll()).hasSize(1);
    }

    private static List<Throwable> runConcurrently(Callable<?> first, Callable<?> second) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch startSignal = new CountDownLatch(1);
            Future<Throwable> firstResult = pool.submit(attempt(first, startSignal));
            Future<Throwable> secondResult = pool.submit(attempt(second, startSignal));

            startSignal.countDown();

            return Stream.of(firstResult.get(30, TimeUnit.SECONDS), secondResult.get(30, TimeUnit.SECONDS))
                    .filter(Objects::nonNull)
                    .toList();
        } finally {
            pool.shutdownNow();
        }
    }

    private static Callable<Throwable> attempt(Callable<?> call, CountDownLatch startSignal) {
        return () -> {
            startSignal.await();
            try {
                call.call();
                return null;
            } catch (Throwable thrown) {
                return thrown;
            }
        };
    }

    private static LoanApplication pendingApplication() {
        return LoanApplication.builder()
                .customer(Customer.builder()
                        .firstName("Customer")
                        .lastName("Surname")
                        .email("customer@example.com")
                        .build())
                .amount(new BigDecimal("50000.00"))
                .loanTerms(24)
                .status(LoanApplicationStatus.PENDING)
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

    private static LenderOfferRequest offerRequest() {
        return new LenderOfferRequest(
                "Lender A", new BigDecimal("5.90"), new BigDecimal("1200.00"), new BigDecimal("43200.00"));
    }
}
