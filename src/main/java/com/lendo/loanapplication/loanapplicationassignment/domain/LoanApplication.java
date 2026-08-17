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
 * A customer's request for a loan.
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
    @Setter
    private LoanApplicationStatus status = LoanApplicationStatus.PENDING;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL)
    @Builder.Default
    private List<LenderOffer> offers = new ArrayList<>();
}
