package com.certainlyinstock.cart_service.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.certainlyinstock.cart_service.dto.CartItemRequest;
import com.certainlyinstock.cart_service.dto.CartItemResponse;
import com.certainlyinstock.cart_service.dto.CartResponse;
import com.certainlyinstock.cart_service.service.CartService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users/{userId}/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<CartResponse> getCart(
            @PathVariable Long userId) {

        return ResponseEntity.ok(
                cartService.getCartByUserId(userId));
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
            @PathVariable Long userId,
            @RequestBody CartItemRequest request) {

        CartItemResponse item = cartService.addItem(
                userId,
                request.getProductId(),
                request.getQuantity());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(item);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartItemResponse> updateItemQuantity(
            @PathVariable Long userId,
            @PathVariable Long productId,
            @RequestBody CartItemRequest request) {

        CartItemResponse item = cartService.updateItemQuantity(
                userId,
                productId,
                request.getQuantity());

        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(
            @PathVariable Long userId,
            @PathVariable Long productId) {

        cartService.removeItem(userId, productId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/items")
    public ResponseEntity<Void> clearCart(
            @PathVariable Long userId) {

        cartService.clearCart(userId);

        return ResponseEntity.noContent().build();
    }
}