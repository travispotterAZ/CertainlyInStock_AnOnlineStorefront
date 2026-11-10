package com.certainlyinstock.auth_service.dto;

import java.util.List;

/** The caller's identity as seen in their access token. keycloakId is the token's "sub" claim. */
public record MeResponse(String keycloakId, String email, String firstName, String lastName, List<String> roles) {
}
