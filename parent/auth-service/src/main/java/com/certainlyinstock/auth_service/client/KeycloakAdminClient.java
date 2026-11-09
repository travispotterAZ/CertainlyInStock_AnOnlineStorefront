package com.certainlyinstock.auth_service.client;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.certainlyinstock.auth_service.config.ClientProperties;
import com.certainlyinstock.auth_service.dto.RegisterRequest;
import com.certainlyinstock.auth_service.exception.EmailAlreadyExistsException;
import com.certainlyinstock.auth_service.exception.InvalidRegistrationException;
import com.certainlyinstock.auth_service.exception.RegistrationFailedException;

/**
 * Talks to Keycloak as the auth-service service account (client-credentials grant). There is
 * no end-user token during registration because the user does not exist yet.
 */
@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() { };

    private final RestClient rest;
    private final ClientProperties.Keycloak props;

    private String cachedToken;
    private Instant cachedTokenExpiry = Instant.EPOCH;

    public KeycloakAdminClient(RestClient.Builder builder, ClientProperties.Keycloak props) {
        this.props = props;
        this.rest = builder.baseUrl(props.baseUrl()).build();
    }

    /** Service-account access token, cached until shortly before it expires. */
    public synchronized String serviceToken() {
        if (cachedToken != null && Instant.now().isBefore(cachedTokenExpiry.minusSeconds(30))) {
            return cachedToken;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", props.clientId());
        form.add("client_secret", props.clientSecret());
        try {
            Map<String, Object> body = rest.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", props.realm())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(MAP);
            if (body == null || !(body.get("access_token") instanceof String token)) {
                throw new RegistrationFailedException("Keycloak token response had no access_token");
            }
            long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 60;
            cachedToken = token;
            cachedTokenExpiry = Instant.now().plusSeconds(expiresIn);
            return token;
        } catch (RestClientException e) {
            throw new RegistrationFailedException("Could not get a Keycloak service token", e);
        }
    }

    /** Creates the user (with the realm's default role, CUSTOMER) and returns its Keycloak id. */
    public String createUser(RegisterRequest request, String email) {
        Map<String, Object> payload = Map.of(
                "username", email,
                "email", email,
                "firstName", request.firstName(),
                "lastName", request.lastName(),
                "enabled", true,
                "credentials", List.of(Map.of("type", "password", "value", request.password(), "temporary", false)));
        String token = serviceToken();
        try {
            return rest.post()
                    .uri("/admin/realms/{realm}/users", props.realm())
                    .headers(h -> h.setBearerAuth(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .exchange((req, res) -> {
                        int status = res.getStatusCode().value();
                        if (status == 201) {
                            return idFromLocation(res.getHeaders().getFirst("Location"));
                        }
                        if (status == 409) {
                            throw new EmailAlreadyExistsException(email);
                        }
                        if (status == 400) {
                            throw new InvalidRegistrationException(errorMessage(res.bodyTo(MAP)));
                        }
                        throw new RegistrationFailedException("Keycloak user creation returned HTTP " + status);
                    });
        } catch (RestClientException e) {
            throw new RegistrationFailedException("Could not reach Keycloak to create the user", e);
        }
    }

    /** Best-effort rollback; failures are logged, not thrown, so the original error is reported. */
    public void deleteUser(String keycloakId) {
        try {
            rest.delete()
                    .uri("/admin/realms/{realm}/users/{id}", props.realm(), keycloakId)
                    .headers(h -> h.setBearerAuth(serviceToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RuntimeException e) {
            log.error("Could not roll back Keycloak user {}; remove it manually", keycloakId, e);
        }
    }

    private static String idFromLocation(String location) {
        if (location == null || location.isBlank()) {
            throw new RegistrationFailedException("Keycloak did not return a Location header");
        }
        return location.substring(location.lastIndexOf('/') + 1);
    }

    private static String errorMessage(Map<String, Object> body) {
        if (body != null && body.get("errorMessage") instanceof String message && !message.isBlank()) {
            return message;
        }
        return "The registration details were rejected";
    }
}
