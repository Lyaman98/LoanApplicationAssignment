package com.lendo.loanapplication.loanapplicationassignment.service.impl;


import com.lendo.loanapplication.loanapplicationassignment.config.LoanPolicyProperties;
import com.lendo.loanapplication.loanapplicationassignment.domain.*;
import com.lendo.loanapplication.loanapplicationassignment.dto.CreateLoanApplicationRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationResponse;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationSummaryResponse;
import com.lendo.loanapplication.loanapplicationassignment.exception.*;
import com.lendo.loanapplication.loanapplicationassignment.repository.LoanApplicationRepository;
import com.lendo.loanapplication.loanapplicationassignment.service.LoanApplicationService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApplicationServiceImpl implements LoanApplicationService {

    private final LoanApplicationRepository loanApplicationRepository;
    private final LoanPolicyProperties loanPolicy;

    @Override
    @Transactional
    public LoanApplicationResponse create(CreateLoanApplicationRequest request) {
        validateRequest(request.amount(), request.loanTerms());
        Instant now = Instant.now();

        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email().trim().toLowerCase(Locale.ROOT))
                .build();



        LoanApplication application = loanApplicationRepository.save(LoanApplication.builder()
                .customer(customer)
                .amount(request.amount())
                .loanTerms(request.loanTerms())
                .status(LoanApplicationStatus.PENDING)
                .expiresAt(now.plus(loanPolicy.applicationExpiration()))
                .build());

        log.info("Loan application created: applicationId={}, status={}", application.getId(), application.getStatus());
        return LoanApplicationResponse.from(application);
    }


    @Override
    @Transactional(readOnly = true)
    public LoanApplicationResponse getById(UUID id) {
        return loanApplicationRepository
                .findWithOffersById(id)
                .map(LoanApplicationResponse::from)
                .orElseThrow(() -> new LoanApplicationNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoanApplicationSummaryResponse> getAll(
            LoanApplicationStatus status, Instant createdFrom, Instant createdTo, Pageable pageable) {
        validateDateRange(createdFrom, createdTo);
        return loanApplicationRepository
                .findAll(filterBy(status, createdFrom, createdTo), pageable)
                .map(LoanApplicationSummaryResponse::from);
    }

    @Override
    @Transactional
    public LoanApplicationResponse acceptLenderOffer(UUID applicationId, UUID offerId) {
        LoanApplication application = loanApplicationRepository
                .findByIdForUpdate(applicationId)
                .orElseThrow(() -> new LoanApplicationNotFoundException(applicationId));

        if (application.getStatus() != LoanApplicationStatus.PENDING) {
            throw new ApplicationNotOpenException(applicationId, application.getStatus());
        }

        LenderOffer acceptedOffer = application.getOffers().stream()
                .filter(offer -> offer.getId().equals(offerId) && offer.getStatus() != LenderOfferStatus.PENDING)
                .findFirst()
                .orElseThrow(() -> new LenderOfferNotFoundException(applicationId, offerId));


        acceptedOffer.setStatus(LenderOfferStatus.ACCEPTED);
        application.getOffers().stream()
                .filter(offer -> !offer.getId().equals(offerId))
                .filter(offer -> offer.getStatus() == LenderOfferStatus.PENDING)
                .forEach(offer -> offer.setStatus(LenderOfferStatus.REJECTED));

        application.setStatus(LoanApplicationStatus.ACCEPTED);
        application.setAcceptedAt(Instant.now());

        log.info("Offer accepted: applicationId={}, offerId={}", applicationId, offerId);

        return LoanApplicationResponse.from(application);
    }


    private void validateRequest(BigDecimal amount, int loanTerms) {
        if (amount.compareTo(loanPolicy.minAmount()) < 0 || amount.compareTo(loanPolicy.maxAmount()) > 0) {
            throw new LoanPolicyViolationException("Requested amount must be between %s and %s"
                    .formatted(loanPolicy.minAmount(), loanPolicy.maxAmount()));
        }
        if (loanTerms < loanPolicy.minLoanTerms() || loanTerms > loanPolicy.maxLoanTerms()) {
            throw new LoanPolicyViolationException("Loan term must be between %d and %d months"
                    .formatted(loanPolicy.minLoanTerms(), loanPolicy.maxLoanTerms()));
        }
    }

    private Specification<LoanApplication> filterBy(
            LoanApplicationStatus status, Instant createdFrom, Instant createdTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (createdFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), createdTo));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validateDateRange(Instant createdFrom, Instant createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new InvalidFilterException("createdFrom must not be after createdTo");
        }
    }

}
