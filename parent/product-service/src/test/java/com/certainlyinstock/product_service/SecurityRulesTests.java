package com.certainlyinstock.product_service;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private static final String PRODUCTS = "/certainlyinstock/product-service/products";

    @Test
    void browsingProductsIsPublic() throws Exception {
        mvc.perform(get(PRODUCTS)).andExpect(status().isOk());
    }

    @Test
    void creatingAProductRequiresAToken() throws Exception {
        mvc.perform(post(PRODUCTS).contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customersCannotCreateProducts() throws Exception {
        mvc.perform(post(PRODUCTS).with(role("CUSTOMER")).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }
}
