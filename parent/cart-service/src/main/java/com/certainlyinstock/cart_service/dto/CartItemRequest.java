package com.certainlyinstock.cart_service.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CartItemRequest {

    private Long productId;
    private Integer quantity;
}
