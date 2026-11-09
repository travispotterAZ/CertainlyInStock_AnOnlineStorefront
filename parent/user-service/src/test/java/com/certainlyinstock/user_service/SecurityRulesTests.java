package com.certainlyinstock.user_service;

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

@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTests {

    @Autowired
    private MockMvc mvc;

    private static RequestPostProcessor role(String role) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Test
    void listingUsersRequiresAToken() throws Exception {
        mvc.perform(get("/certainlyinstock/users/getAll")).andExpect(status().isUnauthorized());
    }

    @Test
    void customersCannotListUsers() throws Exception {
        mvc.perform(get("/certainlyinstock/users/getAll").with(role("CUSTOMER"))).andExpect(status().isForbidden());
    }

    @Test
    void adminsCanListUsers() throws Exception {
        mvc.perform(get("/certainlyinstock/users/getAll").with(role("ADMIN"))).andExpect(status().isOk());
    }
}
