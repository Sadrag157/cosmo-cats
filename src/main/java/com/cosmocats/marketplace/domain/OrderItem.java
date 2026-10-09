package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;


public record OrderItem(Long productId, String productName, int quantity, BigDecimal unitPriceSnapshot) {


    public BigDecimal subtotal() {
        return unitPriceSnapshot.multiply(BigDecimal.valueOf(quantity));
    }
}