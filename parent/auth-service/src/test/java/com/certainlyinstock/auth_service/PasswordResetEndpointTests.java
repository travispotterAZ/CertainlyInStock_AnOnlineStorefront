package com.certainlyinstock.auth_service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.certainlyinstock.auth_service.service.PasswordResetService;

@SpringBootTest(properties = "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks")
@AutoConfigureMockMvc
class PasswordResetEndpointTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void acceptsWithoutATokenAndReturnsNoBody() throws Exception {
        mvc.perform(post("/auth/password-reset").contentType("application/json")
                        .content("{\"email\":\"jane@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));

        verify(passwordResetService).requestReset("jane@example.com");
    }

    @Test
    void anAddressWithNoAccountGetsTheIdenticalResponse() throws Exception {
        // The controller cannot know whether the account exists; the response is the same either way.
        mvc.perform(post("/auth/password-reset").contentType("application/json")
                        .content("{\"email\":\"nobody@example.com\"}"))
                .andExpect(status().isAccepted())
                .andExpect(content().string(""));
    }

    @Test
    void malformedEmailIsRejected() throws Exception {
        mvc.perform(post("/auth/password-reset").contentType("application/json")
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(passwordResetService);
    }
}
