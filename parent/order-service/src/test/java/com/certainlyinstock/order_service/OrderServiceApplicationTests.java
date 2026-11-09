package com.certainlyinstock.order_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks" })
class OrderServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
