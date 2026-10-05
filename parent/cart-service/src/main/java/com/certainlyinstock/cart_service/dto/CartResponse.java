package com.certainlyinstock.cart_service.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CartResponse {

    private Long userId;
    private List<CartItemResponse> items;
}
