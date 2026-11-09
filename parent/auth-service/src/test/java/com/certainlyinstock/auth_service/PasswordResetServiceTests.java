package com.certainlyinstock.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.certainlyinstock.auth_service.client.KeycloakAdminClient;
import com.certainlyinstock.auth_service.exception.RegistrationFailedException;
import com.certainlyinstock.auth_service.service.PasswordResetService;
import com.certainlyinstock.auth_service.service.ResetThrottle;

class PasswordResetServiceTests {

    private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
    private PasswordResetService service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(keycloak, new ResetThrottle());
    }

    @Test
    void existingAccountGetsAResetEmail() {
        when(keycloak.findUserIdByEmail("jane@example.com")).thenReturn(Optional.of("kc-123"));

        service.requestReset("  Jane@Example.COM ");

        verify(keycloak).sendPasswordResetEmail("kc-123");
    }

    @Test
    void unknownAccountSendsNothingAndDoesNotFail() {
        when(keycloak.findUserIdByEmail(any())).thenReturn(Optional.empty());

        assertThatCode(() -> service.requestReset("nobody@example.com")).doesNotThrowAnyException();

        verify(keycloak, never()).sendPasswordResetEmail(any());
    }

    @Test
    void keycloakFailuresAreSwallowedSoNothingLeaksToTheCaller() {
        when(keycloak.findUserIdByEmail(any())).thenThrow(new RegistrationFailedException("keycloak down"));
        assertThatCode(() -> service.requestReset("a@example.com")).doesNotThrowAnyException();

        org.mockito.Mockito.doReturn(Optional.of("kc-9")).when(keycloak).findUserIdByEmail("b@example.com");
        doThrow(new RegistrationFailedException("smtp down")).when(keycloak).sendPasswordResetEmail("kc-9");
        assertThatCode(() -> service.requestReset("b@example.com")).doesNotThrowAnyException();
    }

    @Test
    void repeatRequestsForTheSameAddressSendOnlyOneEmail() {
        when(keycloak.findUserIdByEmail("jane@example.com")).thenReturn(Optional.of("kc-123"));

        service.requestReset("jane@example.com");
        service.requestReset("JANE@example.com");
        service.requestReset("jane@example.com");

        verify(keycloak, times(1)).sendPasswordResetEmail("kc-123");
    }

    @Test
    void throttleAllowsAgainAfterTheCooldownAndTracksAddressesSeparately() {
        Instant start = Instant.parse("2026-01-01T00:00:00Z");
        Clock[] now = { Clock.fixed(start, ZoneOffset.UTC) };
        ResetThrottle throttle = new ResetThrottle(Duration.ofSeconds(60), new Clock() {
            @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return now[0].instant(); }
        });

        assertThat(throttle.tryAcquire("a@example.com")).isTrue();
        assertThat(throttle.tryAcquire("a@example.com")).isFalse();
        assertThat(throttle.tryAcquire("b@example.com")).isTrue();

        now[0] = Clock.fixed(start.plusSeconds(61), ZoneOffset.UTC);
        assertThat(throttle.tryAcquire("a@example.com")).isTrue();
    }
}
