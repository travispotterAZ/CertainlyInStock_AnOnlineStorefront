package com.certainlyinstock.order_service.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.certainlyinstock.order_service.dto.CartView;

/**
 * Reads a user's cart from cart-service. cart-service requires a CUSTOMER or ADMIN token,
 * so this only works because the caller's token is forwarded
 * ({@link com.certainlyinstock.order_service.config.BearerTokenPropagationInterceptor}).
 */
@Component
public class CartClient {

	private final RestClient cartRestClient;

	public CartClient(RestClient cartRestClient) {
		this.cartRestClient = cartRestClient;
	}

	public CartView getCart(Long userId) {
		return cartRestClient.get()
				.uri("/users/{userId}/cart", userId)
				.retrieve()
				.body(CartView.class);
	}
}
