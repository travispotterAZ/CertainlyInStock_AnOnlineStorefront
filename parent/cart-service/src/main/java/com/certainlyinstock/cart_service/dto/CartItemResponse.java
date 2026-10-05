package com.certainlyinstock.cart_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CartItemResponse {

    private Long productId;
    private Integer quantity;
}
