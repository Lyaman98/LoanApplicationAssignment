package com.lendo.loanapplication.loanapplicationassignment.domain;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * A lender's proposed terms on one application.
 */
@Entity
@Table(name = "lender_offer")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LenderOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private LoanApplication application;

    @Column(name = "lender_name", nullable = false)
    private String lenderName;

    @Column(name = "annual_interest_rate", nullable = false)
    private BigDecimal annualInterestRate;

    @Column(name = "monthly_payment_amount", nullable = false)
    private BigDecimal monthlyPaymentAmount;

    @Column(name = "total_repayment", nullable = false)
    private BigDecimal totalRepayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LenderOfferStatus status = LenderOfferStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isPending() {
        return status == LenderOfferStatus.PENDING;
    }

    public void accept() {
        requirePending();
        status = LenderOfferStatus.ACCEPTED;
    }

    public void reject() {
        requirePending();
        status = LenderOfferStatus.REJECTED;
    }

    private void requirePending() {
        if (status != LenderOfferStatus.PENDING) {
            throw new IllegalStateException("Offer %s is already %s".formatted(id, status));
        }
    }
}
