package com.certainlyinstock.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 128) String password,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName) {

    /** Never include the password in logs or error output. */
    @Override
    public String toString() {
        return "RegisterRequest[email=" + email + ", firstName=" + firstName + ", lastName=" + lastName + "]";
    }
}
