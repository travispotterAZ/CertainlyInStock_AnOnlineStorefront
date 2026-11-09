package com.certainlyinstock.auth_service.exception;

/** Keycloak rejected the registration data, e.g. the password breaks the realm's password policy. */
public class InvalidRegistrationException extends RuntimeException {

    public InvalidRegistrationException(String message) {
        super(message);
    }
}
