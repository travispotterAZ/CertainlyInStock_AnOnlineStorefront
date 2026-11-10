package com.certainlyinstock.order_service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks")
@AutoConfigureMockMvc
class SecurityRulesTests {

    @Autowired
    private MockMvc mvc;

    private static RequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void paymentsRequireAToken() throws Exception {
        mvc.perform(get("/api/payments")).andExpect(status().isUnauthorized());
    }

    @Test
    void customersCannotListAllPayments() throws Exception {
        mvc.perform(get("/api/payments").with(role("CUSTOMER"))).andExpect(status().isForbidden());
    }

    @Test
    void adminsCanListAllPayments() throws Exception {
        mvc.perform(get("/api/payments").with(role("ADMIN"))).andExpect(status().isOk());
    }
}
