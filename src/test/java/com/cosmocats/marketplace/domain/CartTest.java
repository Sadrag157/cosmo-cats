package com.cosmocats.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CartTest {

    @Test
    void totalSumsLineSubtotalsWithBigDecimal() {
        Cart cart = new Cart(1L, List.of(
                new CartItem(1L, 2, new BigDecimal("19.99")),
                new CartItem(2L, 1, new BigDecimal("4.50"))));

        assertThat(cart.total()).isEqualByComparingTo("44.48");
    }

    @Test
    void totalOfEmptyCartIsZero() {
        assertThat(new Cart(1L, List.of()).total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void cartItemSubtotalMultipliesPriceByQuantity() {
        assertThat(new CartItem(1L, 3, new BigDecimal("0.10")).subtotal()).isEqualByComparingTo("0.30");
    }

    @Test
    void cartKeepsDefensiveCopyOfItems() {
        List<CartItem> items = new ArrayList<>();
        items.add(new CartItem(1L, 1, new BigDecimal("1.00")));
        Cart cart = new Cart(1L, items);

        items.add(new CartItem(2L, 1, new BigDecimal("2.00")));

        assertThat(cart.items()).hasSize(1);
    }
}