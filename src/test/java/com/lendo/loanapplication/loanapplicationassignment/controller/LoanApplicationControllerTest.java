package com.lendo.loanapplication.loanapplicationassignment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lendo.loanapplication.loanapplicationassignment.domain.LenderOfferStatus;
import com.lendo.loanapplication.loanapplicationassignment.domain.LoanApplicationStatus;
import com.lendo.loanapplication.loanapplicationassignment.dto.*;
import com.lendo.loanapplication.loanapplicationassignment.service.LoanApplicationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@AutoConfigureMockMvc(addFilters = false) // security is covered end to end in AuthenticationIT
@WebMvcTest(LoanApplicationController.class)
class LoanApplicationControllerTest {

    private static final String BASE_PATH = "/api/v1/applications";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private LoanApplicationService loanApplicationService;

    @Test
    void createReturns201WithLocationHeaderAndBody() throws Exception {
        UUID id = UUID.randomUUID();
        when(loanApplicationService.create(any())).thenReturn(response(id, LoanApplicationStatus.PENDING));

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost" + BASE_PATH + "/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.customer.email").value("jane.doe@example.com"));
    }

    @ParameterizedTest(name = "invalid request rejected with 400: {0}")
    @MethodSource("invalidRequests")
    void createRejectsInvalidInputWith400(String caseName, CreateLoanApplicationRequest request) throws Exception {
        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Object[]> invalidRequests() {
        return Stream.of(
                new Object[] {"blank first name", request("", "Doe", "jane.doe@example.com", "50000", 24)},
                new Object[] {"blank last name", request("Jane", " ", "jane.doe@example.com", "50000", 24)},
                new Object[] {"malformed email", request("Jane", "Doe", "not-an-email", "50000", 24)},
                new Object[] {"negative amount", request("Jane", "Doe", "jane.doe@example.com", "-1", 24)},
                new Object[] {"zero amount", request("Jane", "Doe", "jane.doe@example.com", "0", 24)},
                new Object[] {"zero loan terms", request("Jane", "Doe", "jane.doe@example.com", "50000", 0)});
    }

    @Test
    void createRejectsMalformedJsonWith400() throws Exception {
        mockMvc.perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON).content("{\"amount\": }"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getByIdReturnsApplicationWithOffers() throws Exception {
        UUID id = UUID.randomUUID();
        when(loanApplicationService.getById(id)).thenReturn(response(id, LoanApplicationStatus.PENDING));

        mockMvc.perform(get(BASE_PATH + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.offers").isArray());
    }

    @Test
    void getByIdRejectsMalformedUuidWith400() throws Exception {
        mockMvc.perform(get(BASE_PATH + "/not-a-uuid")).andExpect(status().isBadRequest());
    }

    @Test
    void getAllPassesFiltersAndPaginationToService() throws Exception {
        Page<LoanApplicationSummaryResponse> page =
                new PageImpl<>(List.of(summary()), PageRequest.of(1, 5), 11);
        when(loanApplicationService.getAll(any(), any(), any(), any())).thenReturn(page);

        mockMvc.perform(get(BASE_PATH)
                        .param("status", "PENDING")
                        .param("createdFrom", "2026-01-01T00:00:00Z")
                        .param("createdTo", "2026-02-01T00:00:00Z")
                        .param("page", "1")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(11));

        ArgumentCaptor<LoanApplicationStatus> statusCaptor = ArgumentCaptor.forClass(LoanApplicationStatus.class);
        ArgumentCaptor<Instant> fromCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Instant> toCaptor = ArgumentCaptor.forClass(Instant.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(loanApplicationService)
                .getAll(statusCaptor.capture(), fromCaptor.capture(), toCaptor.capture(), pageableCaptor.capture());

        assertThat(statusCaptor.getValue()).isEqualTo(LoanApplicationStatus.PENDING);
        assertThat(fromCaptor.getValue()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(toCaptor.getValue()).isEqualTo(Instant.parse("2026-02-01T00:00:00Z"));
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(5);
    }

    @Test
    void getAllWorksWithNoFilters() throws Exception {
        when(loanApplicationService.getAll(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk());

        ArgumentCaptor<LoanApplicationStatus> statusCaptor = ArgumentCaptor.forClass(LoanApplicationStatus.class);
        verify(loanApplicationService).getAll(statusCaptor.capture(), any(), any(), any());
        assertThat(statusCaptor.getValue()).isNull();
    }

    @Test
    void getAllRejectsUnknownStatusWith400() throws Exception {
        mockMvc.perform(get(BASE_PATH).param("status", "NOT_A_STATUS")).andExpect(status().isBadRequest());
    }

    private static CreateLoanApplicationRequest validRequest() {
        return request("Jane", "Doe", "jane.doe@example.com", "50000.00", 24);
    }

    private static CreateLoanApplicationRequest request(
            String firstName, String lastName, String email, String amount, int loanTerms) {
        return new CreateLoanApplicationRequest(firstName, lastName, email, new BigDecimal(amount), loanTerms);
    }

    private static LoanApplicationResponse response(UUID id, LoanApplicationStatus status) {
        return new LoanApplicationResponse(
                id,
                new CustomerResponse("Jane", "Doe", "jane.doe@example.com"),
                new BigDecimal("50000.00"),
                24,
                status,
                Instant.parse("2026-01-15T10:00:00Z"),
                List.of());
    }

    private static LoanApplicationSummaryResponse summary() {
        return new LoanApplicationSummaryResponse(
                UUID.randomUUID(),
                new CustomerResponse("Jane", "Doe", "jane.doe@example.com"),
                new BigDecimal("50000.00"),
                24,
                LoanApplicationStatus.PENDING,
                Instant.parse("2026-01-15T10:00:00Z"));
    }
}
