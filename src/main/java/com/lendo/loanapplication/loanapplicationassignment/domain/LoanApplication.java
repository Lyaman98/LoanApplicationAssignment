package com.lendo.loanapplication.loanapplicationassignment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A customer's request for a loan, and the aggregate root for the offers made on it.
 */
@Entity
@Table(name = "loan_application")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Embedded
    private Customer customer;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(name = "loan_terms", nullable = false)
    private int loanTerms;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LoanApplicationStatus status = LoanApplicationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LenderOffer> offers = new ArrayList<>();

    /** Offers may be submitted, and one of them accepted, only while the application is pending. */
    public boolean isOpenForOffers() {
        return status != LoanApplicationStatus.PENDING;
    }

    /***
     * <p>Called at the start of every write, so an expired application can never take an offer or an
     * acceptance. There is no background job: an application that nobody touches keeps reporting
     * PENDING on reads until a write arrives. See the README for why that trade-off was made.
     */
    public void setAsExpiredIfStale(Instant now) {
        if (status == LoanApplicationStatus.PENDING && !now.isBefore(expiresAt)) {
            status = LoanApplicationStatus.EXPIRED;
            expiredAt = now;
        }
    }

    public void accept(Instant now) {
        if (status != LoanApplicationStatus.PENDING) {
            throw new IllegalStateException("Cannot accept an offer on application %s: it is %s".formatted(id, status));
        }
        status = LoanApplicationStatus.ACCEPTED;
        acceptedAt = now;
    }
}
