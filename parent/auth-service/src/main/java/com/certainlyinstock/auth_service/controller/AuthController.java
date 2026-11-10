package com.certainlyinstock.auth_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.certainlyinstock.auth_service.dto.MeResponse;
import com.certainlyinstock.auth_service.dto.PasswordResetRequest;
import com.certainlyinstock.auth_service.dto.RegisterRequest;
import com.certainlyinstock.auth_service.dto.RegisterResponse;
import com.certainlyinstock.auth_service.service.PasswordResetService;
import com.certainlyinstock.auth_service.service.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegistrationService registrationService;
    private final PasswordResetService passwordResetService;

    public AuthController(RegistrationService registrationService, PasswordResetService passwordResetService) {
        this.registrationService = registrationService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Who am I? Echoes the identity in the caller's validated token; handy for the frontend and
     * for checking a token without touching a database. Login itself happens at Keycloak.
     */
    @GetMapping("/me")
    public MeResponse me(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring("ROLE_".length()))
                .sorted()
                .toList();
        return new MeResponse(jwt.getSubject(), jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"), jwt.getClaimAsString("family_name"), roles);
    }

    /** Public endpoint (see SecurityConfig): the caller has no token yet. Always creates a CUSTOMER. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }

    /**
     * Public endpoint. Always answers 202 with no body, whether or not the address has an
     * account, so it cannot be used to discover who is registered. If the account exists,
     * Keycloak emails a single-use link; the work happens in the background.
     */
    @PostMapping("/password-reset")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        passwordResetService.requestReset(request.email());
    }
}
