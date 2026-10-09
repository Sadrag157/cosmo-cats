package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;
import java.util.List;

public record Order(Long id, List<OrderItem> items, OrderStatus status) {

    public Order {
        items = List.copyOf(items);
    }

    public BigDecimal total() {
        return items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}