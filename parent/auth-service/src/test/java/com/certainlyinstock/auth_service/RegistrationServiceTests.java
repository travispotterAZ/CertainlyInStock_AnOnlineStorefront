package com.certainlyinstock.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.certainlyinstock.auth_service.client.KeycloakAdminClient;
import com.certainlyinstock.auth_service.client.UserServiceClient;
import com.certainlyinstock.auth_service.dto.RegisterRequest;
import com.certainlyinstock.auth_service.dto.RegisterResponse;
import com.certainlyinstock.auth_service.exception.EmailAlreadyExistsException;
import com.certainlyinstock.auth_service.exception.RegistrationFailedException;
import com.certainlyinstock.auth_service.service.RegistrationService;

class RegistrationServiceTests {

    private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
    private final UserServiceClient userService = mock(UserServiceClient.class);
    private RegistrationService service;
    private final RegisterRequest request = new RegisterRequest("  Jane@Example.COM ", "secret-pass", "Jane", "Doe");

    @BeforeEach
    void setUp() {
        service = new RegistrationService(keycloak, userService);
    }

    @Test
    void createsKeycloakUserThenProfileUsingTheSameLowerCaseEmail() {
        when(keycloak.createUser(any(), eq("jane@example.com"))).thenReturn("kc-123");

        RegisterResponse response = service.register(request);

        assertThat(response).isEqualTo(new RegisterResponse("kc-123", "jane@example.com"));
        verify(userService).createProfile("kc-123", request, "jane@example.com");
        verify(keycloak, never()).deleteUser(any());
    }

    @Test
    void deletesTheKeycloakUserWhenProfileCreationFails() {
        when(keycloak.createUser(any(), any())).thenReturn("kc-123");
        RegistrationFailedException failure = new RegistrationFailedException("user-service down");
        org.mockito.Mockito.doThrow(failure).when(userService).createProfile(any(), any(), any());

        assertThatThrownBy(() -> service.register(request)).isSameAs(failure);

        verify(keycloak).deleteUser("kc-123");
    }

    @Test
    void duplicateProfileEmailAlsoRollsBackTheKeycloakUser() {
        when(keycloak.createUser(any(), any())).thenReturn("kc-123");
        org.mockito.Mockito.doThrow(new EmailAlreadyExistsException("jane@example.com"))
                .when(userService).createProfile(any(), any(), any());

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyExistsException.class);

        verify(keycloak).deleteUser("kc-123");
    }

    @Test
    void doesNotCreateAProfileWhenKeycloakRejectsTheUser() {
        when(keycloak.createUser(any(), any())).thenThrow(new EmailAlreadyExistsException("jane@example.com"));

        assertThatThrownBy(() -> service.register(request)).isInstanceOf(EmailAlreadyExistsException.class);

        verify(userService, never()).createProfile(any(), any(), any());
        verify(keycloak, never()).deleteUser(any());
    }

    @Test
    void requestToStringDoesNotLeakThePassword() {
        assertThat(request.toString()).doesNotContain("secret-pass");
    }
}
