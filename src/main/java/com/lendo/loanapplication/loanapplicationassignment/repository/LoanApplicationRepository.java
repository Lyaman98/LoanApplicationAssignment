package com.lendo.loanapplication.loanapplicationassignment.repository;

import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplication;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface LoanApplicationRepository
        extends JpaRepository<LoanApplication, UUID>, JpaSpecificationExecutor<LoanApplication> {

    /**
     * Fetches the application with its offers in one query
     */
    @EntityGraph(attributePaths = "offers")
    Optional<LoanApplication> findWithOffersById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from LoanApplication a where a.id = :id")
    Optional<LoanApplication> findByIdForUpdate(@Param("id") UUID id);
}
