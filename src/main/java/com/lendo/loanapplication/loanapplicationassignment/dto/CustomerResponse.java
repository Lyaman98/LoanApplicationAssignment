package com.lendo.loanapplication.loanapplicationassignment.dto;

import com.lendo.loanapplication.loanapplicationassignment.domain.Customer;

public record CustomerResponse(String firstName, String lastName, String email) {

    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(customer.getFirstName(), customer.getLastName(), customer.getEmail());
    }
}
