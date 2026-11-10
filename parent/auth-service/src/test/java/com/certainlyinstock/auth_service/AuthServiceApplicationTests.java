package com.certainlyinstock.auth_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"eureka.client.enabled=false",
		"spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:9/jwks" })
class AuthServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
