package com.lendo.loanapplication.loanapplicationassignment.controller;


import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import com.lendo.loanapplication.loanapplicationassignment.dto.CreateLoanApplicationRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationResponse;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationSummaryResponse;
import com.lendo.loanapplication.loanapplicationassignment.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/v1/applications")
@Tag(name = "Loan applications", description = "Create, look up and close loan applications")
@RequiredArgsConstructor
public class LoanApplicationController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final LoanApplicationService loanApplicationService;

    @Operation(
            summary = "Creates a loan application.",
            description =
                    "Creates a loan application in `PENDING` status and opens it for lender offers. The amount and "
                            + "term are checked against the configured loan policy.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Created. The Location header points at the new application.",
                content = @Content(schema = @Schema(implementation = LoanApplicationResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Malformed body, failed validation, or an amount or term outside the loan policy.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = APPLICATION_JSON_VALUE, produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<LoanApplicationResponse> create(@Valid @RequestBody CreateLoanApplicationRequest request) {
        LoanApplicationResponse created = loanApplicationService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @Operation(
            summary = "Returns one loan application with all of its offers.",
            description = "Offers are fetched together with the application in a single query.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "The application, including every offer made on it.",
                content = @Content(schema = @Schema(implementation = LoanApplicationResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "No application with that id.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public LoanApplicationResponse getById(@PathVariable UUID id) {
        return loanApplicationService.getById(id);
    }

    @Operation(
            summary = "Lists loan applications.",
            description =
                    "Optionally filtered by status and by a creation window; both bounds are inclusive and either "
                            + "may be omitted. Results are paged and sorted by creation date, newest first. Summaries "
                            + "do not include offers - fetch a single application for those.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "A page of matching applications."),
        @ApiResponse(
                responseCode = "400",
                description = "Unknown status value, unparseable timestamp, or createdFrom after createdTo.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public Page<LoanApplicationSummaryResponse> getAll(
            @RequestParam(required = false) LoanApplicationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant createdTo,
            @ParameterObject @PageableDefault(sort = "createdAt", direction = DESC) Pageable pageable) {
        return loanApplicationService.getAll(status, createdFrom, createdTo, pageable);
    }
}
