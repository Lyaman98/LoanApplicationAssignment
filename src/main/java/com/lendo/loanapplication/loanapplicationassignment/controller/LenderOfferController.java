package com.lendo.loanapplication.loanapplicationassignment.controller;

import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferResponse;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationResponse;
import com.lendo.loanapplication.loanapplicationassignment.service.LenderOfferService;
import com.lendo.loanapplication.loanapplicationassignment.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/applications/{applicationId}/offers")
@Tag(name = "Lender offers", description = "Offers submitted by partner lenders")
@RequiredArgsConstructor
public class LenderOfferController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final LenderOfferService lenderOfferService;
    private final LoanApplicationService loanApplicationService;

    @Operation(
            summary = "Submits a lender's offer on an application.",
            description =
                    "Offers are only accepted while the application is PENDING, and each lender may make at most "
                            + "one offer per application. The lender name is matched after trimming, and the same "
                            + "rule is enforced by a unique constraint in the database.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                description = "Created. The Location header points at the new offer.",
                content = @Content(schema = @Schema(implementation = LenderOfferResponse.class))),
        @ApiResponse(
                responseCode = "400",
                description = "Malformed body or failed validation.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "404",
                description = "No application with that id.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description =
                        "The application is no longer PENDING, or this lender has already offered on it.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
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

    @Operation(
            summary = "Accepts one offer and closes the application.",
            description =
                    "Moves the application to `ACCEPTED`, marks the chosen offer `ACCEPTED` and every other pending "
                            + "offer `REJECTED`. The application row is locked for the duration, so two customers "
                            + "accepting at the same moment cannot both succeed - the second request gets a 409.")
    @ApiResponses({
        @ApiResponse(
                responseCode = "200",
                description = "The closed application, with every offer in its final state.",
                content = @Content(schema = @Schema(implementation = LoanApplicationResponse.class))),
        @ApiResponse(
                responseCode = "404",
                description = "No application with that id, or that offer is not on this application.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class))),
        @ApiResponse(
                responseCode = "409",
                description = "The application is no longer PENDING, or the offer has already been decided.",
                content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{offerId}/accept")
    public LoanApplicationResponse acceptLenderOffer(@PathVariable UUID applicationId, @PathVariable UUID offerId) {
        return loanApplicationService.acceptLenderOffer(applicationId, offerId);
    }
}
