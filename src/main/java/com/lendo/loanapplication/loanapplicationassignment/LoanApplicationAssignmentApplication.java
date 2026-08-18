package com.lendo.loanapplication.loanapplicationassignment;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@SecurityScheme(name = "basicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@OpenAPIDefinition(
        security = @SecurityRequirement(name = "basicAuth"),
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
