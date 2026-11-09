package com.certainlyinstock.auth_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import com.certainlyinstock.auth_service.config.KeycloakRoleConverter;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks")
@AutoConfigureMockMvc
class SecurityConfigTests {

    @Autowired
    private MockMvc mvc;

    @Test
    void healthEndpointIsPublic() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    void protectedEndpointRejectsMissingToken() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void validTokenPassesSecurity() throws Exception {
        // No controller exists yet, so a request that gets past security ends in 404, not 401/403.
        mvc.perform(get("/auth/me").with(jwt())).andExpect(status().isNotFound());
    }

    @Test
    void keycloakRealmRolesBecomeSpringAuthorities() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("ADMIN", "CUSTOMER")))
                .build();

        assertThat(new KeycloakRoleConverter().convert(token))
                .extracting(Object::toString)
                .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_CUSTOMER");
    }

    @Test
    void tokenWithoutRealmAccessGetsNoAuthorities() {
        Jwt token = Jwt.withTokenValue("t").header("alg", "none").claim("sub", "x").build();

        assertThat(new KeycloakRoleConverter().convert(token)).isEmpty();
    }
}
