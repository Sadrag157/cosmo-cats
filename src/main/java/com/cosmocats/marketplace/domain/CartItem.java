package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;

public record CartItem(Long productId, int quantity, BigDecimal unitPrice) {

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}