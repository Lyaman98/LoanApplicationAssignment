package com.lendo.loanapplication.loanapplicationassignment.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lendo.loanapplication.loanapplicationassignment.AbstractPostgresTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationIT extends AbstractPostgresTest {

    private static final String APPLICATIONS = "/api/v1/applications";
    private static final String CUSTOMER = basic("customer", "password");
    private static final String LENDER = basic("lender", "password");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void rejectsAnUnauthenticatedRequest() throws Exception {
        mockMvc.perform(get(APPLICATIONS)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongCredentials() throws Exception {
        mockMvc.perform(get(APPLICATIONS).header("Authorization", basic("customer", "wrong")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void eachRoleCanOnlyDoItsOwnPartOfTheFlow() throws Exception {
        // A lender may not create an application.
        mockMvc.perform(post(APPLICATIONS)
                        .header("Authorization", LENDER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody()))
                .andExpect(status().isForbidden());

        String applicationId = idFrom(mockMvc.perform(post(APPLICATIONS)
                        .header("Authorization", CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applicationBody()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // A customer may not submit an offer.
        mockMvc.perform(post(APPLICATIONS + "/{id}/offers", applicationId)
                        .header("Authorization", CUSTOMER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerBody()))
                .andExpect(status().isForbidden());

        String offerId = idFrom(mockMvc.perform(post(APPLICATIONS + "/{id}/offers", applicationId)
                        .header("Authorization", LENDER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(offerBody()))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString());

        // A lender may not accept on the customer's behalf.
        mockMvc.perform(post(APPLICATIONS + "/{id}/offers/{offerId}/accept", applicationId, offerId)
                        .header("Authorization", LENDER))
                .andExpect(status().isForbidden());

        mockMvc.perform(post(APPLICATIONS + "/{id}/offers/{offerId}/accept", applicationId, offerId)
                        .header("Authorization", CUSTOMER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));
    }

    private String idFrom(String responseBody) throws Exception {
        return objectMapper.readTree(responseBody).get("id").asText();
    }

    private static String basic(String username, String password) {
        String credentials = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
    }

    private static String applicationBody() {
        return """
                {"firstName":"Jane","lastName":"Doe","email":"jane.doe@example.com",
                 "amount":50000.00,"loanTerms":24}""";
    }

    private static String offerBody() {
        return """
                {"lenderName":"Lender A","annualInterestRate":5.90,
                 "monthlyPaymentAmount":1200.00,"totalRepayment":43200.00}""";
    }
}
