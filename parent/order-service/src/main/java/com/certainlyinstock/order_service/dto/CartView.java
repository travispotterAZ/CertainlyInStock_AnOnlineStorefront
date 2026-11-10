package com.certainlyinstock.order_service.dto;

import java.util.List;

/** cart-service's cart, as returned by GET /users/{userId}/cart. */
public record CartView(Long userId, List<Line> items) {

	public record Line(Long productId, Integer quantity) {
	}
}
