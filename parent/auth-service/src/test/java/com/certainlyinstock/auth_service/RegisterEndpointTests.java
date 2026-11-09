package com.certainlyinstock.auth_service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.certainlyinstock.auth_service.dto.RegisterResponse;
import com.certainlyinstock.auth_service.exception.EmailAlreadyExistsException;
import com.certainlyinstock.auth_service.exception.InvalidRegistrationException;
import com.certainlyinstock.auth_service.exception.RegistrationFailedException;
import com.certainlyinstock.auth_service.service.RegistrationService;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks")
@AutoConfigureMockMvc
class RegisterEndpointTests {

    private static final String VALID = """
            {"email":"jane@example.com","password":"secret-pass","firstName":"Jane","lastName":"Doe"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RegistrationService registrationService;

    @Test
    void registersWithoutAnyToken() throws Exception {
        when(registrationService.register(any())).thenReturn(new RegisterResponse("kc-123", "jane@example.com"));

        mvc.perform(post("/auth/register").contentType("application/json").content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.keycloakId").value("kc-123"))
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void invalidInputIsRejectedBeforeAnyBackendCall() throws Exception {
        mvc.perform(post("/auth/register").contentType("application/json")
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"firstName\":\"\",\"lastName\":\"Doe\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registrationService);
    }

    @Test
    void duplicateEmailReturns409() throws Exception {
        when(registrationService.register(any())).thenThrow(new EmailAlreadyExistsException("jane@example.com"));

        mvc.perform(post("/auth/register").contentType("application/json").content(VALID))
                .andExpect(status().isConflict());
    }

    @Test
    void passwordPolicyFailureReturns400WithKeycloakMessage() throws Exception {
        when(registrationService.register(any())).thenThrow(new InvalidRegistrationException("Invalid password: too weak"));

        mvc.perform(post("/auth/register").contentType("application/json").content(VALID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Invalid password: too weak"));
    }

    @Test
    void backendFailureReturns502WithoutLeakingDetails() throws Exception {
        when(registrationService.register(any())).thenThrow(new RegistrationFailedException("keycloak:8080 refused connection"));

        mvc.perform(post("/auth/register").contentType("application/json").content(VALID))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("keycloak:8080"))));
    }
}
