package com.lendo.loanapplication.loanapplicationassignment.controller;

import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferResponse;
import com.lendo.loanapplication.loanapplicationassignment.service.LenderOfferService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/offers")
@RequiredArgsConstructor
public class LenderOfferController {

    private final LenderOfferService lenderOfferService;

    @PostMapping
    public ResponseEntity<LenderOfferResponse> submit(
            @PathVariable UUID applicationId, @Valid @RequestBody LenderOfferRequest request) {
        LenderOfferResponse created = lenderOfferService.submit(applicationId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{offerId}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }
}
