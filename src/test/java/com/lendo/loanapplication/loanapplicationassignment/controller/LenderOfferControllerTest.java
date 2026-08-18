package com.lendo.loanapplication.loanapplicationassignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOfferStatus;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferRequest;
import com.lendo.loanapplication.loanapplicationassignment.dto.LenderOfferResponse;
import com.lendo.loanapplication.loanapplicationassignment.dto.CustomerResponse;
import com.lendo.loanapplication.loanapplicationassignment.dto.LoanApplicationResponse;
import com.lendo.loanapplication.loanapplicationassignment.service.LenderOfferService;
import com.lendo.loanapplication.loanapplicationassignment.service.LoanApplicationService;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc(addFilters = false) // security is covered end to end in AuthenticationIT
@WebMvcTest(LenderOfferController.class)
class LenderOfferControllerTest {

    private static final String OFFERS_PATH = "/api/v1/applications/{applicationId}/offers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LenderOfferService lenderOfferService;

    @MockitoBean
    private LoanApplicationService loanApplicationService;

    @Test
    void submitReturns201WithLocationHeaderAndBody() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        when(lenderOfferService.submit(eq(applicationId), any())).thenReturn(offerResponse(offerId));

        mockMvc.perform(post(OFFERS_PATH, applicationId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header()
                        .string(
                                "Location",
                                "http://localhost/api/v1/applications/" + applicationId + "/offers/" + offerId))
                .andExpect(jsonPath("$.id").value(offerId.toString()))
                .andExpect(jsonPath("$.lenderName").value("Lender A"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @ParameterizedTest(name = "invalid offer rejected with 400: {0}")
    @MethodSource("invalidRequests")
    void submitRejectsInvalidInputWith400(String caseName, LenderOfferRequest request) throws Exception {
        mockMvc.perform(post(OFFERS_PATH, UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Object[]> invalidRequests() {
        return Stream.of(
                new Object[] {"blank lender name", offerRequest("", "5.9", "1200.00", "43200.00")},
                new Object[] {"negative interest rate", offerRequest("Lender A", "-0.1", "1200.00", "43200.00")},
                new Object[] {"interest rate above 100", offerRequest("Lender A", "100.01", "1200.00", "43200.00")},
                new Object[] {"zero monthly payment", offerRequest("Lender A", "5.9", "0.00", "43200.00")},
                new Object[] {"zero total repayment", offerRequest("Lender A", "5.9", "1200.00", "0.00")});
    }

    @Test
    void submitRejectsMissingBodyWith400() throws Exception {
        mockMvc.perform(post(OFFERS_PATH, UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void acceptReturnsUpdatedApplicationWithSiblingsRejected() throws Exception {
        UUID applicationId = UUID.randomUUID();
        UUID offerId = UUID.randomUUID();
        when(loanApplicationService.acceptLenderOffer(applicationId, offerId))
                .thenReturn(acceptedResponse(applicationId, offerId));

        mockMvc.perform(post(OFFERS_PATH + "/{offerId}/accept", applicationId, offerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(applicationId.toString()))
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.offers[0].status").value("ACCEPTED"))
                .andExpect(jsonPath("$.offers[1].status").value("REJECTED"));

        verify(loanApplicationService).acceptLenderOffer(applicationId, offerId);
    }

    @Test
    void acceptRejectsMalformedUuidWith400() throws Exception {
        mockMvc.perform(post(OFFERS_PATH + "/not-a-uuid/accept", UUID.randomUUID()))
                .andExpect(status().isBadRequest());
    }

    private static LenderOfferRequest validRequest() {
        return offerRequest("Lender A", "5.9", "1200.00", "43200.00");
    }

    private static LenderOfferRequest offerRequest(
            String lenderName, String rate, String monthlyPayment, String totalRepayment) {
        return new LenderOfferRequest(
                lenderName, new BigDecimal(rate), new BigDecimal(monthlyPayment), new BigDecimal(totalRepayment));
    }

    private static LenderOfferResponse offerResponse(UUID offerId) {
        return new LenderOfferResponse(
                offerId,
                "Lender A",
                new BigDecimal("5.9"),
                new BigDecimal("1200.00"),
                new BigDecimal("43200.00"),
                LenderOfferStatus.PENDING,
                Instant.parse("2026-01-15T10:00:00Z"));
    }

    private static LoanApplicationResponse acceptedResponse(UUID applicationId, UUID acceptedOfferId) {
        LenderOfferResponse accepted = new LenderOfferResponse(
                acceptedOfferId,
                "Lender A",
                new BigDecimal("5.90"),
                new BigDecimal("1200.00"),
                new BigDecimal("43200.00"),
                LenderOfferStatus.ACCEPTED,
                Instant.parse("2026-01-15T10:00:00Z"));
        LenderOfferResponse rejected = new LenderOfferResponse(
                UUID.randomUUID(),
                "Lender B",
                new BigDecimal("6.40"),
                new BigDecimal("1260.00"),
                new BigDecimal("45360.00"),
                LenderOfferStatus.REJECTED,
                Instant.parse("2026-01-15T10:05:00Z"));

        return new LoanApplicationResponse(
                applicationId,
                new CustomerResponse("Jane", "Doe", "jane.doe@example.com"),
                new BigDecimal("50000.00"),
                24,
                LoanApplicationStatus.ACCEPTED,
                Instant.parse("2026-01-15T09:00:00Z"),
                Stream.of(accepted, rejected).toList());
    }

}
