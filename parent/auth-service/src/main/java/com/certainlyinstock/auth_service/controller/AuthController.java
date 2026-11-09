package com.certainlyinstock.auth_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.certainlyinstock.auth_service.dto.RegisterRequest;
import com.certainlyinstock.auth_service.dto.RegisterResponse;
import com.certainlyinstock.auth_service.service.RegistrationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final RegistrationService registrationService;

    public AuthController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    /** Public endpoint (see SecurityConfig): the caller has no token yet. Always creates a CUSTOMER. */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
        return registrationService.register(request);
    }
}
