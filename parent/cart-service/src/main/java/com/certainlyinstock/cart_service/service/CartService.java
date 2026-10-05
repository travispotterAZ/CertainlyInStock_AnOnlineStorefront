package com.certainlyinstock.cart_service.service;

import com.certainlyinstock.cart_service.dto.CartItemResponse;
import com.certainlyinstock.cart_service.dto.CartResponse;

public interface CartService {

    // Get the user's current cart
    CartResponse getCartByUserId(Long userId);

    // Add a product to the user's cart
    CartItemResponse addItem(Long userId, Long productId, Integer quantity);

    // Update the quantity of a product already in the user's cart
    CartItemResponse updateItemQuantity(
            Long userId,
            Long productId,
            Integer quantity);

    // Remove a product from the user's cart
    void removeItem(Long userId, Long productId);

    // Remove all items from the user's cart
    void clearCart(Long userId);
}