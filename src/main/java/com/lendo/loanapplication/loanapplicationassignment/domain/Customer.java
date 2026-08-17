package com.lendo.loanapplication.loanapplicationassignment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

/**
 * The person applying, as submitted with the application.
 */
@Embeddable
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Customer {

    @Column(name = "first_name", nullable = false)
    @NotEmpty
    private String firstName;

    @Column(name = "last_name", nullable = false)
    @NotEmpty
    private String lastName;

    @Column(nullable = false)
    @NotEmpty
    @Email(message = "Email should be valid")
    private String email;
}
