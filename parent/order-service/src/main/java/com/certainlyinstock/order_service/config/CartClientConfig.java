package com.certainlyinstock.order_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.autoconfigure.RestClientBuilderConfigurer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * HTTP client for calling cart-service. The builder is {@link LoadBalanced}, so
 * {@code http://cart-service} is resolved through Eureka, and it starts from Boot's
 * standard configuration (message converters, observation for tracing).
 *
 * The load-balanced builder is {@code defaultCandidate = false}: it is only injected where
 * explicitly asked for with {@code @LoadBalanced}. Otherwise every other RestClient user
 * would get it too, including the Eureka client, which would then try to resolve the
 * Eureka servers' own hostnames as service names and fail to register.
 */
@Configuration
public class CartClientConfig {

	@Bean(defaultCandidate = false)
	@LoadBalanced
	RestClient.Builder loadBalancedRestClientBuilder(RestClientBuilderConfigurer configurer) {
		return configurer.configure(RestClient.builder());
	}

	@Bean
	RestClient cartRestClient(@LoadBalanced RestClient.Builder loadBalancedRestClientBuilder,
			@Value("${cart-service.base-url:http://cart-service}") String baseUrl) {
		return loadBalancedRestClientBuilder
				.baseUrl(baseUrl)
				.requestInterceptor(new BearerTokenPropagationInterceptor())
				.build();
	}
}
