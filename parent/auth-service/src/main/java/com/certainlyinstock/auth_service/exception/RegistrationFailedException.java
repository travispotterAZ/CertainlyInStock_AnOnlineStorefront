package com.certainlyinstock.auth_service.exception;

/** Keycloak or user-service failed or was unreachable. The message is for logs, not for clients. */
public class RegistrationFailedException extends RuntimeException {

    public RegistrationFailedException(String message) {
        super(message);
    }

    public RegistrationFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
