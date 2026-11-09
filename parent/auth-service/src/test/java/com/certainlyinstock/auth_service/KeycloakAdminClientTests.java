package com.certainlyinstock.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.certainlyinstock.auth_service.client.KeycloakAdminClient;
import com.certainlyinstock.auth_service.config.ClientProperties;
import com.certainlyinstock.auth_service.dto.RegisterRequest;
import com.certainlyinstock.auth_service.exception.EmailAlreadyExistsException;
import com.certainlyinstock.auth_service.exception.InvalidRegistrationException;
import com.certainlyinstock.auth_service.exception.RegistrationFailedException;

class KeycloakAdminClientTests {

    private static final String BASE = "http://keycloak.test";
    private static final String TOKEN_URL = BASE + "/realms/certainlyinstock/protocol/openid-connect/token";
    private static final String USERS_URL = BASE + "/admin/realms/certainlyinstock/users";
    private static final String TOKEN_JSON = "{\"access_token\":\"svc-token\",\"expires_in\":300}";

    private final RegisterRequest request = new RegisterRequest("jane@example.com", "secret-pass", "Jane", "Doe");
    private MockRestServiceServer server;
    private KeycloakAdminClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new KeycloakAdminClient(builder,
                new ClientProperties.Keycloak(BASE, "certainlyinstock", "auth-service", "the-secret"));
    }

    @Test
    void createsUserWithServiceTokenAndReturnsIdFromLocationHeader() {
        server.expect(requestTo(TOKEN_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("grant_type=client_credentials")))
                .andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL)).andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer svc-token"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"temporary\":false")))
                .andRespond(withStatus(HttpStatus.CREATED).header("Location", USERS_URL + "/kc-123"));

        assertThat(client.createUser(request, "jane@example.com")).isEqualTo("kc-123");
        server.verify();
    }

    @Test
    void reusesTheCachedServiceToken() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));

        client.serviceToken();
        client.serviceToken();

        server.verify(); // a second token request would have failed: only one was expected
    }

    @Test
    void duplicateUserBecomesEmailAlreadyExists() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.CONFLICT)
                .contentType(MediaType.APPLICATION_JSON).body("{\"errorMessage\":\"User exists with same username\"}"));

        assertThatThrownBy(() -> client.createUser(request, "jane@example.com"))
                .isInstanceOf(EmailAlreadyExistsException.class);
    }

    @Test
    void passwordPolicyFailureBecomesInvalidRegistrationWithKeycloakMessage() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"errorMessage\":\"Invalid password: minimum length 8.\"}"));

        assertThatThrownBy(() -> client.createUser(request, "jane@example.com"))
                .isInstanceOf(InvalidRegistrationException.class)
                .hasMessage("Invalid password: minimum length 8.");
    }

    @Test
    void keycloakServerErrorBecomesRegistrationFailed() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL)).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.createUser(request, "jane@example.com"))
                .isInstanceOf(RegistrationFailedException.class);
    }

    @Test
    void rejectedServiceCredentialsBecomeRegistrationFailed() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

        assertThatThrownBy(() -> client.serviceToken()).isInstanceOf(RegistrationFailedException.class);
    }

    @Test
    void failedRollbackDoesNotThrow() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL + "/kc-123")).andExpect(method(HttpMethod.DELETE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        client.deleteUser("kc-123"); // must swallow the error so the original failure is reported
        server.verify();
    }

    @Test
    void findsAUserByExactEmailAndEncodesPlusSigns() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL + "?email=jane%2Btest%40example.com&exact=true"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer svc-token"))
                .andRespond(withSuccess("[{\"id\":\"kc-123\",\"email\":\"jane+test@example.com\"}]", MediaType.APPLICATION_JSON));

        assertThat(client.findUserIdByEmail("jane+test@example.com")).contains("kc-123");
        server.verify();
    }

    @Test
    void unknownEmailGivesEmptyResult() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(USERS_URL)))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(client.findUserIdByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void sendsTheUpdatePasswordActionWithALinkLifespan() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(USERS_URL + "/kc-123/execute-actions-email?lifespan=1800"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(header("Authorization", "Bearer svc-token"))
                .andExpect(content().json("[\"UPDATE_PASSWORD\"]"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));

        client.sendPasswordResetEmail("kc-123");
        server.verify();
    }

    @Test
    void emailSendFailureSurfacesAsRegistrationFailed() {
        server.expect(requestTo(TOKEN_URL)).andRespond(withSuccess(TOKEN_JSON, MediaType.APPLICATION_JSON));
        server.expect(requestTo(org.hamcrest.Matchers.startsWith(USERS_URL)))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.sendPasswordResetEmail("kc-123"))
                .isInstanceOf(RegistrationFailedException.class);
    }
}
