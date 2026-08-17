package com.lendo.loanapplication.loanapplicationassignment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LoanApplicationAssignmentApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanApplicationAssignmentApplication.class, args);
    }

}
