package com.lendo.loanapplication.loanapplicationassignment.repository;

import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface LenderOfferRepository extends JpaRepository<LenderOffer, UUID> {

    boolean existsByApplicationIdAndLenderName(UUID applicationId, String lenderName);
}
