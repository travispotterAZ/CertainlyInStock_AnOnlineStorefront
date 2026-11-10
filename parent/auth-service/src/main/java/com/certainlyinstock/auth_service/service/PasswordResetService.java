package com.certainlyinstock.auth_service.service;

import java.util.Locale;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.certainlyinstock.auth_service.client.KeycloakAdminClient;

/**
 * Starts a password reset by asking Keycloak to email a reset link.
 *
 * Nothing here may reveal whether an account exists, so: the controller always answers 202,
 * this runs in the background (a registered address takes longer because mail is sent, which
 * would otherwise show in the response time), and every failure is logged, never thrown.
 */
@Service
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final KeycloakAdminClient keycloak;
    private final ResetThrottle throttle;

    public PasswordResetService(KeycloakAdminClient keycloak, ResetThrottle throttle) {
        this.keycloak = keycloak;
        this.throttle = throttle;
    }

    @Async
    public void requestReset(String rawEmail) {
        String email = rawEmail.trim().toLowerCase(Locale.ROOT);
        try {
            if (!throttle.tryAcquire(email)) {
                log.info("Password reset skipped: request for the same address within the cooldown");
                return;
            }
            Optional<String> userId = keycloak.findUserIdByEmail(email);
            if (userId.isEmpty()) {
                log.info("Password reset requested for an address with no account");
                return;
            }
            keycloak.sendPasswordResetEmail(userId.get());
            log.info("Password reset email requested for user {}", userId.get());
        } catch (RuntimeException e) {
            log.error("Password reset failed", e);
        }
    }
}
