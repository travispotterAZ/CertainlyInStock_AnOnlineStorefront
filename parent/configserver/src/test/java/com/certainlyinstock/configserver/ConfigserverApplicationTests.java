package com.certainlyinstock.configserver;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;

@SpringBootTest(properties = "eureka.client.enabled=false")
class ConfigserverApplicationTests {

	@Autowired
	private EnvironmentRepository repository;

	@Test
	void contextLoads() {
	}

	@Test
	void eachPeerUsesTheOtherTwoPeers() {
		for (int peer = 1; peer <= 3; peer++) {
			Environment environment = repository.findOne("eureka-server", "peer" + peer, null);
			assertThat(property(environment, "eureka.instance.hostname")).isEqualTo("eureka" + peer);
			String urls = (String) property(environment, "eureka.client.serviceUrl.defaultZone");
			assertThat(urls.split(",")).hasSize(2);
			for (int target = 1; target <= 3; target++) {
				String url = "http://eureka" + target + ":8080/eureka/";
				if (target == peer) {
					assertThat(urls).doesNotContain(url);
				} else {
					assertThat(urls).contains(url);
				}
			}
		}
	}

	@Test
	void servicesReceiveRegistrationAndRegistryFetchingSettings() {
		for (String service : List.of("auth-service", "user-service", "product-service",
				"cart-service", "order-service", "api-gateway", "eureka-server")) {
			Environment environment = repository.findOne(service, "default", null);
			assertThat(property(environment, "eureka.client.register-with-eureka")).isEqualTo("true");
			assertThat(property(environment, "eureka.client.fetch-registry")).isEqualTo("true");
			assertThat(property(environment, "eureka.client.serviceUrl.defaultZone")).isNotNull();
		}
	}

	private Object property(Environment environment, String name) {
		return environment.getPropertySources().stream()
				.map(source -> source.getSource().get(name))
				.filter(value -> value != null).findFirst().orElse(null);
	}
}
