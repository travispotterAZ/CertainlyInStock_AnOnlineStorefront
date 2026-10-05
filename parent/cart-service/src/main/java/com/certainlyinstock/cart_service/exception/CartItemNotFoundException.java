package com.certainlyinstock.cart_service.exception;

public class CartItemNotFoundException extends RuntimeException {

    public CartItemNotFoundException(Long productId) {
        super("Product not found in cart with product id: " + productId);
    }
}
