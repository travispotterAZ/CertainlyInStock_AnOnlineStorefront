package com.certainlyinstock.cart_service.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.certainlyinstock.cart_service.dto.CartItemResponse;
import com.certainlyinstock.cart_service.dto.CartResponse;
import com.certainlyinstock.cart_service.entity.Cart;
import com.certainlyinstock.cart_service.entity.CartItem;
import com.certainlyinstock.cart_service.exception.CartItemNotFoundException;
import com.certainlyinstock.cart_service.repository.CartItemRepository;
import com.certainlyinstock.cart_service.repository.CartRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    @Override
    public CartResponse getCartByUserId(Long userId) {
        Cart cart = getOrCreateCart(userId);

        List<CartItemResponse> items = cartItemRepository.findByCartId(cart.getId())
                .stream()
                .map(item -> new CartItemResponse(
                        item.getProductId(),
                        item.getQuantity()))
                .toList();

        return new CartResponse(
                cart.getUserId(),
                items);
    }

    @Override
    public CartItemResponse addItem(
            Long userId,
            Long productId,
            Integer quantity) {

        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .map(existingItem -> {
                    existingItem.setQuantity(
                            existingItem.getQuantity() + quantity);

                    return cartItemRepository.save(existingItem);
                })
                .orElseGet(() -> {
                    CartItem newItem = new CartItem();
                    newItem.setCart(cart);
                    newItem.setProductId(productId);
                    newItem.setQuantity(quantity);

                    return cartItemRepository.save(newItem);
                });

        return toCartItemResponse(item);
    }

    @Override
    public CartItemResponse updateItemQuantity(
            Long userId,
            Long productId,
            Integer quantity) {

        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        item.setQuantity(quantity);

        CartItem updatedItem = cartItemRepository.save(item);

        return toCartItemResponse(updatedItem);
    }

    @Override
    public void removeItem(Long userId, Long productId) {
        Cart cart = getOrCreateCart(userId);

        CartItem item = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElseThrow(() -> new CartItemNotFoundException(productId));

        cartItemRepository.delete(item);
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);

        cartItemRepository.deleteByCartId(cart.getId());
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUserId(userId);

                    return cartRepository.save(cart);
                });
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        return new CartItemResponse(
                item.getProductId(),
                item.getQuantity());
    }
}