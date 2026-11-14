package com.cosmocats.marketplace.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void totalSumsPriceSnapshots() {
        Order order = new Order(1L, "ORD-1", "cat@example.com", List.of(
                new OrderItem(3L, "Comet Tail Scratcher", 1, new BigDecimal("34.00")),
                new OrderItem(2L, "Cosmic Milk", 3, new BigDecimal("4.50"))), OrderStatus.NEW);

        assertThat(order.total()).isEqualByComparingTo("47.50");
        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.orderNumber()).isEqualTo("ORD-1");
        assertThat(order.customerEmail()).isEqualTo("cat@example.com");
    }

    @Test
    void totalOfEmptyOrderIsZero() {
        Order order = new Order(1L, "ORD-2", "cat@example.com", List.of(), OrderStatus.PAID);

        assertThat(order.total()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void orderItemSubtotalUsesPriceSnapshot() {
        OrderItem item = new OrderItem(1L, "Star Mug", 4, new BigDecimal("2.25"));

        assertThat(item.subtotal()).isEqualByComparingTo("9.00");
        assertThat(item.productName()).isEqualTo("Star Mug");
    }

    @Test
    void orderStatusHasAllRequiredValues() {
        assertThat(OrderStatus.values())
                .containsExactly(OrderStatus.NEW, OrderStatus.PAID, OrderStatus.SHIPPED,
                        OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    }

    @Test
    void categoryHoldsItsData() {
        Category category = new Category(1L, "Toys", "Toys for cosmic cats");

        assertThat(category.id()).isEqualTo(1L);
        assertThat(category.name()).isEqualTo("Toys");
    }
}