package com.certainlyinstock.auth_service.client;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.certainlyinstock.auth_service.config.ClientProperties;
import com.certainlyinstock.auth_service.dto.RegisterRequest;
import com.certainlyinstock.auth_service.exception.EmailAlreadyExistsException;
import com.certainlyinstock.auth_service.exception.RegistrationFailedException;

/** Creates the profile row in user-service, authenticating with the service-account token. */
@Component
public class UserServiceClient {

    private final RestClient rest;
    private final KeycloakAdminClient keycloak;

    public UserServiceClient(RestClient.Builder builder, ClientProperties.UserService props, KeycloakAdminClient keycloak) {
        this.rest = builder.baseUrl(props.baseUrl()).build();
        this.keycloak = keycloak;
    }

    public void createProfile(String keycloakId, RegisterRequest request, String email) {
        Map<String, Object> payload = Map.of(
                "keycloakId", keycloakId,
                "email", email,
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "role", "CUSTOMER");
        String token = keycloak.serviceToken();
        try {
            rest.post()
                    .uri("/certainlyinstock/users/create")
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((req, res) -> {
                        int status = res.getStatusCode().value();
                        if (status == 201) {
                            return null;
                        }
                        if (status == 409) {
                            throw new EmailAlreadyExistsException(email);
                        }
                        throw new RegistrationFailedException("user-service profile creation returned HTTP " + status);
                    });
        } catch (RestClientException e) {
            throw new RegistrationFailedException("Could not reach user-service to create the profile", e);
        }
    }
}
