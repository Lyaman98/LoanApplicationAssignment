package com.lendo.loanapplication.loanapplicationassignment.controller;


import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import com.lendo.loanapplication.loanapplicationassignment.dto.CreateLoanApplicationRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationResponse;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationSummaryResponse;
import com.lendo.loanapplication.loanapplicationassignment.service.LoanApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/v1/applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private final LoanApplicationService loanApplicationService;

    // TODO: Add Swagger
    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanApplicationResponse> create(@Valid @RequestBody CreateLoanApplicationRequest request) {
        LoanApplicationResponse created = loanApplicationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    // TODO: Add Swagger
    @GetMapping("/{id}")
    public LoanApplicationResponse getById(@PathVariable UUID id) {
        return loanApplicationService.getById(id);
    }

    @GetMapping
    public Page<LoanApplicationSummaryResponse> getAll(
            @RequestParam(required = false) LoanApplicationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            Pageable pageable) {
        return loanApplicationService.getAll(status, createdFrom, createdTo, pageable);
    }

    // TODO: Add Swagger
    @PostMapping("/{id}/offers/{offerId}/accept")
    public LoanApplicationResponse acceptLenderOffer(@PathVariable UUID id, @PathVariable UUID offerId) {
        return loanApplicationService.acceptLenderOffer(id, offerId);
    }
}
