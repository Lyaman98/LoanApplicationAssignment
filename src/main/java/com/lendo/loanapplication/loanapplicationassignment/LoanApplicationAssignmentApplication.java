package com.lendo.loanapplication.loanapplicationassignment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@OpenAPIDefinition(
        info =
                @Info(
                        title = "Loan Comparison Platform API",
                        version = "v1",
                        description = "Customers submit loan applications, lenders respond with offers, "
                                + "and the customer accepts one."))
public class LoanApplicationAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanApplicationAssignmentApplication.class, args);
    }

}
