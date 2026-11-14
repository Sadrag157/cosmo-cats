package com.cosmocats.marketplace.persistence.entity;

import com.cosmocats.marketplace.domain.OrderItem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "order_items_seq")
    @SequenceGenerator(name = "order_items_seq", sequenceName = "order_items_seq", allocationSize = 1)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity purchaseOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceSnapshot;

    protected OrderItemEntity() {
    }

    OrderItemEntity(OrderEntity purchaseOrder, ProductEntity product, int quantity, BigDecimal unitPriceSnapshot) {
        this.purchaseOrder = purchaseOrder;
        this.product = product;
        this.quantity = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
    }

    OrderItem toDomain() {
        return new OrderItem(product.getId(), product.getName(), quantity, unitPriceSnapshot);
    }
}