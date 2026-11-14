package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderNotFoundException;
import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.domain.ProductSales;
import com.cosmocats.marketplace.persistence.entity.OrderEntity;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.repository.OrderItemRepository;
import com.cosmocats.marketplace.persistence.repository.OrderRepository;
import com.cosmocats.marketplace.persistence.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final EntityManager entityManager;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                        ProductRepository productRepository, EntityManager entityManager) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional
    public Order placeOrder(String customerEmail, Map<Long, Integer> quantitiesByProductId) {
        if (quantitiesByProductId.isEmpty()) {
            throw new IllegalArgumentException("An order must contain at least one item");
        }
        OrderEntity order = new OrderEntity(UUID.randomUUID().toString(), customerEmail, OrderStatus.NEW);
        quantitiesByProductId.forEach((productId, quantity) -> {
            if (quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be positive for product " + productId);
            }
            ProductEntity product = productRepository.findById(productId)
                    .orElseThrow(() -> new ProductNotFoundException(productId));
            order.addItem(product, quantity);
        });
        return orderRepository.save(order).toDomain();
    }

    @Transactional(readOnly = true)
    public Order findById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("id " + id))
                .toDomain();
    }

    @Transactional(readOnly = true)
    public Order findByOrderNumber(String orderNumber) {
        OrderEntity order = entityManager.unwrap(Session.class)
                .bySimpleNaturalId(OrderEntity.class)
                .load(orderNumber);
        if (order == null) {
            throw new OrderNotFoundException("number " + orderNumber);
        }
        return order.toDomain();
    }

    @Transactional
    public Order updateStatus(Long id, OrderStatus status) {
        OrderEntity order = orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException("id " + id));
        order.setStatus(status);
        return order.toDomain();
    }

    @Transactional
    public void delete(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new OrderNotFoundException("id " + id);
        }
        orderRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<ProductSales> findBestSellingProducts(int limit) {
        return orderItemRepository.findBestSellingProducts(OrderStatus.CANCELLED, PageRequest.of(0, limit)).stream()
                .map(view -> new ProductSales(view.getProductId(), view.getProductName(), view.getTotalQuantity()))
                .toList();
    }
}