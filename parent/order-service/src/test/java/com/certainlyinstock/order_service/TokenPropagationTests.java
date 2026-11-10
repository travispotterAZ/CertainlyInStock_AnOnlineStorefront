package com.certainlyinstock.order_service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.sun.net.httpserver.HttpServer;

/**
 * order-service's checkout preview calls cart-service on the user's behalf. A stub
 * cart-service, registered as "cart-service" in the discovery client so the load-balanced
 * RestClient finds it, records the Authorization header and, like the real cart-service,
 * refuses requests without a token.
 */
@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks" })
@AutoConfigureMockMvc
class TokenPropagationTests {

	private static final String CART_JSON =
			"{\"userId\":1,\"items\":[{\"productId\":5,\"quantity\":2},{\"productId\":9,\"quantity\":1}]}";

	private static final List<String> receivedAuthHeaders = new CopyOnWriteArrayList<>();
	private static final List<String> receivedPaths = new CopyOnWriteArrayList<>();
	private static final HttpServer cartService = startCartService();

	@Autowired
	private MockMvc mvc;

	private static HttpServer startCartService() {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		server.createContext("/", exchange -> {
			String auth = exchange.getRequestHeaders().getFirst("Authorization");
			receivedAuthHeaders.add(String.valueOf(auth));
			receivedPaths.add(exchange.getRequestURI().getPath());
			int status;
			byte[] body = new byte[0];
			if (auth == null) {
				status = 401;
			} else if (auth.equals("Bearer refused-token")) {
				status = 403;
			} else {
				status = 200;
				body = CART_JSON.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().add("Content-Type", "application/json");
			}
			exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
			if (body.length > 0) {
				exchange.getResponseBody().write(body);
			}
			exchange.close();
		});
		server.start();
		return server;
	}

	@DynamicPropertySource
	static void cartServiceInstance(DynamicPropertyRegistry registry) {
		registry.add("spring.cloud.discovery.client.simple.instances.cart-service[0].uri",
				() -> "http://127.0.0.1:" + cartService.getAddress().getPort());
	}

	@AfterAll
	static void stopCartService() {
		cartService.stop(0);
	}

	@BeforeEach
	void clear() {
		receivedAuthHeaders.clear();
		receivedPaths.clear();
	}

	private static RequestPostProcessor user(String tokenValue, String role) {
		return jwt().jwt(token -> token.tokenValue(tokenValue).claim("preferred_username", "customer@test"))
				.authorities(new SimpleGrantedAuthority("ROLE_" + role));
	}

	@Test
	void customersOwnTokenIsForwardedToCartService() throws Exception {
		mvc.perform(get("/api/payments/checkout-preview/1").with(user("customer-token-abc", "CUSTOMER")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.userId").value(1))
				.andExpect(jsonPath("$.itemCount").value(3))
				.andExpect(jsonPath("$.items.length()").value(2));

		assertThat(receivedPaths).containsExactly("/users/1/cart");
		assertThat(receivedAuthHeaders).containsExactly("Bearer customer-token-abc");
	}

	@Test
	void adminsTokenIsForwardedToo() throws Exception {
		mvc.perform(get("/api/payments/checkout-preview/1").with(user("admin-token-xyz", "ADMIN")))
				.andExpect(status().isOk());

		assertThat(receivedAuthHeaders).containsExactly("Bearer admin-token-xyz");
	}

	@Test
	void requestWithoutATokenIsRejectedBeforeCartServiceIsCalled() throws Exception {
		mvc.perform(get("/api/payments/checkout-preview/1")).andExpect(status().isUnauthorized());

		assertThat(receivedPaths).isEmpty();
	}

	@Test
	void cartServicesRefusalIsPassedBackToTheCaller() throws Exception {
		mvc.perform(get("/api/payments/checkout-preview/1").with(user("refused-token", "CUSTOMER")))
				.andExpect(status().isForbidden());

		assertThat(receivedAuthHeaders).containsExactly("Bearer refused-token");
	}
}
