package com.certainlyinstock.auth_service.dto;

/** keycloakId is the Keycloak user id, which is also the "sub" claim in that user's tokens. */
public record RegisterResponse(String keycloakId, String email) {
}
