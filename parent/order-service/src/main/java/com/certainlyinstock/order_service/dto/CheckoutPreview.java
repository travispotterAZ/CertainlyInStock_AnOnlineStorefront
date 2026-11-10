package com.certainlyinstock.order_service.dto;

import java.util.List;

/** What the customer is about to pay for, built from their cart in cart-service. */
public record CheckoutPreview(Long userId, int itemCount, List<CartView.Line> items) {

	public static CheckoutPreview from(CartView cart) {
		List<CartView.Line> items = cart.items() == null ? List.of() : cart.items();
		int count = items.stream().mapToInt(line -> line.quantity() == null ? 0 : line.quantity()).sum();
		return new CheckoutPreview(cart.userId(), count, items);
	}
}
