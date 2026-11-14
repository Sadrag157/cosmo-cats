package com.cosmocats.marketplace.persistence.entity;

import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import org.hibernate.annotations.NaturalId;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "orders_seq")
    @SequenceGenerator(name = "orders_seq", sequenceName = "orders_seq", allocationSize = 1)
    private Long id;

    @NaturalId
    @Column(name = "order_number", nullable = false, updatable = false, length = 36)
    private String orderNumber;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItemEntity> items = new ArrayList<>();

    protected OrderEntity() {
    }

    public OrderEntity(String orderNumber, String customerEmail, OrderStatus status) {
        this.orderNumber = orderNumber;
        this.customerEmail = customerEmail;
        this.status = status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public void addItem(ProductEntity product, int quantity) {
        items.add(new OrderItemEntity(this, product, quantity, product.getPrice()));
    }

    public Order toDomain() {
        return new Order(id, orderNumber, customerEmail,
                items.stream().map(OrderItemEntity::toDomain).toList(), status);
    }
}