package com.certainlyinstock.cart_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class SecurityRulesTests {

    @Autowired
    private MockMvc mvc;

    private static RequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void cartRequiresAToken() throws Exception {
        mvc.perform(get("/users/1/cart")).andExpect(status().isUnauthorized());
    }

    @Test
    void customersPassSecurity() throws Exception {
        int status = mvc.perform(get("/users/1/cart").with(role("CUSTOMER"))).andReturn().getResponse().getStatus();
        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void tokensWithoutAStoreRoleAreForbidden() throws Exception {
        mvc.perform(get("/users/1/cart").with(role("SOMETHING_ELSE"))).andExpect(status().isForbidden());
    }
}
