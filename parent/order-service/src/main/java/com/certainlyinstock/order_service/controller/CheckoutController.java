package com.certainlyinstock.order_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.certainlyinstock.order_service.client.CartClient;
import com.certainlyinstock.order_service.dto.CheckoutPreview;

/**
 * Example of user-token propagation: order-service calls cart-service on the user's
 * behalf, forwarding the user's own bearer token.
 */
@RestController
@PreAuthorize("hasAnyRole('CUSTOMER','ADMIN')")
@RequestMapping("/api/payments/checkout-preview")
public class CheckoutController {

	private final CartClient cartClient;

	public CheckoutController(CartClient cartClient) {
		this.cartClient = cartClient;
	}

	@GetMapping("/{userId}")
	public ResponseEntity<CheckoutPreview> preview(@PathVariable Long userId) {
		try {
			return ResponseEntity.ok(CheckoutPreview.from(cartClient.getCart(userId)));
		} catch (RestClientResponseException e) {
			// cart-service answered with an error (for example 401/403 if the token was refused).
			return ResponseEntity.status(e.getStatusCode()).build();
		} catch (RestClientException e) {
			// cart-service could not be reached or found in Eureka.
			return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
		}
	}
}
