package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.util.List;


public record Cart(Long id, List<CartItem> items) {

    public Cart {
        items = List.copyOf(items);
    }


    public BigDecimal total() {
        return items.stream()
                .map(CartItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}