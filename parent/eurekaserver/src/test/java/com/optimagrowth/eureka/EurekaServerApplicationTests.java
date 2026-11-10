package com.optimagrowth.eureka;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
		"spring.config.import=",
		"spring.cloud.config.enabled=false",
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false"
})
class EurekaServerApplicationTests {

	@LocalServerPort
	private int port;

	@Test
	void servesTheRegistry() throws Exception {
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/eureka/apps"))
					.header("Accept", "application/json").build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			assertThat(response.statusCode()).isEqualTo(200);
			assertThat(response.body()).contains("applications");
		}
	}

}
