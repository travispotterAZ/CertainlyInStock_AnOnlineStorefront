package com.certainlyinstock.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.sun.net.httpserver.HttpServer;

/**
 * Loads the real route definitions from the config server's api-gateway.properties,
 * resolves each lb:// service through a stub discovery registry and checks which
 * path the backend receives.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class GatewayRoutingTest {

	private static final Path ROUTE_FILE =
			Path.of("../configserver/src/main/resources/config-repo/api-gateway.properties");

	private static final List<String> receivedPaths = new CopyOnWriteArrayList<>();
	private static final List<String> receivedAuthHeaders = new CopyOnWriteArrayList<>();
	private static final List<String> receivedMethods = new CopyOnWriteArrayList<>();
	private static final HttpServer backend = startBackend();

	@Autowired
	private WebTestClient client;

	private static HttpServer startBackend() {
		HttpServer server;
		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		server.createContext("/", exchange -> {
			receivedPaths.add(exchange.getRequestURI().getRawPath());
			receivedMethods.add(exchange.getRequestMethod());
			receivedAuthHeaders.add(String.valueOf(exchange.getRequestHeaders().getFirst("Authorization")));
			byte[] body = exchange.getRequestURI().getRawPath().getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});
		server.start();
		return server;
	}

	@AfterAll
	static void stopBackend() {
		backend.stop(0);
	}

	@BeforeEach
	void clear() {
		receivedPaths.clear();
		receivedAuthHeaders.clear();
		receivedMethods.clear();
	}

	// Use the real routes, including lb:// URIs, with an isolated discovery registry.
	@DynamicPropertySource
	static void routes(DynamicPropertyRegistry registry) throws IOException {
		Properties routes = new Properties();
		try (InputStream in = Files.newInputStream(ROUTE_FILE)) {
			routes.load(in);
		}
		String stubUri = "http://127.0.0.1:" + backend.getAddress().getPort();
		routes.stringPropertyNames().forEach(key -> {
			String value = routes.getProperty(key);
			registry.add(key, () -> value);
		});
		// Register the stub as the only instance of every service a route names, so new
		// routes are covered without editing this list.
		routes.stringPropertyNames().stream()
				.filter(key -> key.endsWith(".uri") && routes.getProperty(key).startsWith("lb://"))
				.map(key -> routes.getProperty(key).substring("lb://".length()))
				.distinct()
				.forEach(service -> registry.add(
						"spring.cloud.discovery.client.simple.instances." + service + "[0].uri", () -> stubUri));
	}

	private void assertForwarded(String requestPath, String expectedBackendPath) {
		client.get().uri(requestPath).exchange()
				.expectStatus().isOk()
				.expectBody(String.class).isEqualTo(expectedBackendPath);
		assertThat(receivedPaths).containsExactly(expectedBackendPath);
	}

	@Test
	void cartRoutePassesPathThroughUnchanged() {
		assertForwarded("/users/1/cart", "/users/1/cart");
		receivedPaths.clear();
		assertForwarded("/users/1/cart/items/9", "/users/1/cart/items/9");
	}

	@Test
	void userRouteIsForwarded() {
		assertForwarded("/certainlyinstock/users/getAll", "/certainlyinstock/users/getAll");
	}

	@Test
	void productRouteIsForwarded() {
		assertForwarded("/certainlyinstock/product-service/products/5",
				"/certainlyinstock/product-service/products/5");
	}

	@Test
	void orderRouteServesPayments() {
		assertForwarded("/api/payments/order/7", "/api/payments/order/7");
	}

	@Test
	void authRegisterIsForwardedWithoutAToken() {
		client.post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"email\":\"new@example.com\"}")
				.exchange().expectStatus().isOk()
				.expectBody(String.class).isEqualTo("/auth/register");
		assertThat(receivedPaths).containsExactly("/auth/register");
		assertThat(receivedMethods).containsExactly("POST");
		assertThat(receivedAuthHeaders).containsExactly("null");
	}

	@Test
	void authPasswordResetIsForwarded() {
		client.post().uri("/auth/password-reset").contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"email\":\"new@example.com\"}")
				.exchange().expectStatus().isOk();
		assertThat(receivedPaths).containsExactly("/auth/password-reset");
		assertThat(receivedMethods).containsExactly("POST");
	}

	@Test
	void authMeIsForwardedWithTheTokenUnchanged() {
		client.get().uri("/auth/me").header("Authorization", "Bearer abc.def.ghi")
				.exchange().expectStatus().isOk()
				.expectBody(String.class).isEqualTo("/auth/me");
		assertThat(receivedPaths).containsExactly("/auth/me");
		assertThat(receivedMethods).containsExactly("GET");
		assertThat(receivedAuthHeaders).containsExactly("Bearer abc.def.ghi");
	}

	@Test
	void unknownPathsAreNotForwarded() {
		client.get().uri("/nope").exchange().expectStatus().isNotFound();
		client.get().uri("/cart/users/1/cart").exchange().expectStatus().isNotFound();
		client.get().uri("/users/1").exchange().expectStatus().isNotFound();
		client.get().uri("/authx").exchange().expectStatus().isNotFound();
		assertThat(receivedPaths).isEmpty();
	}

	@Test
	void authorizationHeaderReachesBackendUnchanged() {
		client.get().uri("/api/payments").header("Authorization", "Bearer abc.def.ghi")
				.exchange().expectStatus().isOk();
		assertThat(receivedAuthHeaders).containsExactly("Bearer abc.def.ghi");
	}
}
