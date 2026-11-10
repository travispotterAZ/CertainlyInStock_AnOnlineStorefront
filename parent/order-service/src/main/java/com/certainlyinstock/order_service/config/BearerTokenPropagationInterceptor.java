package com.certainlyinstock.order_service.config;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 * Forwards the caller's bearer token on outgoing service-to-service calls, so the
 * downstream service sees the same user (and roles) as this one did.
 *
 * The Boot 4 equivalent of Chapter 9's KeycloakRestTemplate / UserContextInterceptor.
 * SecurityContextHolder is thread-local: calls made on another thread (@Async) need the
 * security context copied to that thread first.
 */
public class BearerTokenPropagationInterceptor implements ClientHttpRequestInterceptor {

	private static final Logger log = LoggerFactory.getLogger(BearerTokenPropagationInterceptor.class);

	@Override
	public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
			throws IOException {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof JwtAuthenticationToken jwtAuth) {
			request.getHeaders().setBearerAuth(jwtAuth.getToken().getTokenValue());
			log.info("Propagating bearer token of user '{}' to {} {}", jwtAuth.getName(), request.getMethod(),
					request.getURI());
		} else {
			log.warn("No JWT in the security context; calling {} {} without a token", request.getMethod(),
					request.getURI());
		}
		return execution.execute(request, body);
	}
}
