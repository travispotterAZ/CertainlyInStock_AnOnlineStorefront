package com.certainlyinstock.api_gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

/**
 * The "docker" profile file repeats every route in full (Spring replaces a list as a
 * whole when it is defined in two places), so the two files can drift apart. This keeps
 * their routes identical, and checks every route resolves its service through Eureka.
 */
class RouteFilesInSyncTest {

	private static final Path CONFIG_REPO = Path.of("../configserver/src/main/resources/config-repo");
	private static final String ROUTE_PREFIX = "spring.cloud.gateway.";

	private static Map<String, String> routeProperties(String fileName) throws IOException {
		Properties properties = new Properties();
		try (InputStream in = Files.newInputStream(CONFIG_REPO.resolve(fileName))) {
			properties.load(in);
		}
		Map<String, String> routes = new TreeMap<>();
		properties.stringPropertyNames().stream()
				.filter(key -> key.startsWith(ROUTE_PREFIX))
				.forEach(key -> routes.put(key, properties.getProperty(key)));
		return routes;
	}

	@Test
	void dockerFileDefinesTheSameRoutesAsTheBaseFile() throws IOException {
		Map<String, String> base = routeProperties("api-gateway.properties");
		Map<String, String> docker = routeProperties("api-gateway-docker.properties");

		assertThat(docker).isEqualTo(base);
	}

	@Test
	void everyRouteResolvesItsServiceThroughEureka() throws IOException {
		Map<String, String> base = routeProperties("api-gateway.properties");

		base.forEach((key, value) -> {
			if (key.endsWith(".id")) {
				String uriKey = key.substring(0, key.length() - ".id".length()) + ".uri";
				assertThat(base.get(uriKey)).as(uriKey).isEqualTo("lb://" + value);
			}
		});
	}
}
