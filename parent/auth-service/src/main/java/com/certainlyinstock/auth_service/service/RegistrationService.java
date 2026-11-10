package com.certainlyinstock.auth_service.service;

import java.util.Locale;

import org.springframework.stereotype.Service;

import com.certainlyinstock.auth_service.client.KeycloakAdminClient;
import com.certainlyinstock.auth_service.client.UserServiceClient;
import com.certainlyinstock.auth_service.dto.RegisterRequest;
import com.certainlyinstock.auth_service.dto.RegisterResponse;

/**
 * Registration spans two systems: Keycloak (identity and password) and user-service (profile).
 * They cannot share a transaction, so if the profile step fails the Keycloak user is deleted.
 */
@Service
public class RegistrationService {

    private final KeycloakAdminClient keycloak;
    private final UserServiceClient userService;

    public RegistrationService(KeycloakAdminClient keycloak, UserServiceClient userService) {
        this.keycloak = keycloak;
        this.userService = userService;
    }

    public RegisterResponse register(RegisterRequest request) {
        // Keycloak stores emails lower-case; use the same value in both systems.
        String email = request.email().trim().toLowerCase(Locale.ROOT);

        String keycloakId = keycloak.createUser(request, email);
        try {
            userService.createProfile(keycloakId, request, email);
        } catch (RuntimeException e) {
            keycloak.deleteUser(keycloakId);
            throw e;
        }
        return new RegisterResponse(keycloakId, email);
    }
}
