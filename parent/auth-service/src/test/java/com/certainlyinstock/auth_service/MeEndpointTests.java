package com.certainlyinstock.auth_service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks")
@AutoConfigureMockMvc
class MeEndpointTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsTheCallersIdentityAndRoles() throws Exception {
        mvc.perform(get("/auth/me").with(jwt()
                        .jwt(token -> token.subject("kc-123").claim("email", "jane@example.com")
                                .claim("given_name", "Jane").claim("family_name", "Doe"))
                        .authorities(new SimpleGrantedAuthority("ROLE_CUSTOMER"),
                                new SimpleGrantedAuthority("ROLE_ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keycloakId").value("kc-123"))
                .andExpect(jsonPath("$.email").value("jane@example.com"))
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.lastName").value("Doe"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.roles[1]").value("CUSTOMER"));
    }

    @Test
    void requiresAToken() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }
}
