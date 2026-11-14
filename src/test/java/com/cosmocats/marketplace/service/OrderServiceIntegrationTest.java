package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.TestcontainersConfiguration;
import com.cosmocats.marketplace.domain.Order;
import com.cosmocats.marketplace.domain.OrderItem;
import com.cosmocats.marketplace.domain.OrderNotFoundException;
import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.domain.ProductSales;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import com.cosmocats.marketplace.persistence.repository.OrderRepository;
import com.cosmocats.marketplace.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, OrderService.class})
class OrderServiceIntegrationTest {

    private static final String EMAIL = "cat@example.com";

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private CategoryEntity category;
    private ProductEntity yarn;
    private ProductEntity milk;

    @BeforeEach
    void setUp() {
        category = categoryRepository.save(new CategoryEntity("Cat Toys", "Toys"));
        yarn = productRepository.save(new ProductEntity("Anti-Gravity Yarn Ball", "Yarn",
                new BigDecimal("19.99"), category, 100));
        milk = productRepository.save(new ProductEntity("Cosmic Milk", "Milk",
                new BigDecimal("4.50"), category, 100));
    }

    @Test
    void placeOrderSnapshotsPricesAndCalculatesTotal() {
        Order order = orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 2, milk.getId(), 1));

        assertThat(order.id()).isNotNull();
        assertThat(order.orderNumber()).isNotBlank();
        assertThat(order.status()).isEqualTo(OrderStatus.NEW);
        assertThat(order.customerEmail()).isEqualTo(EMAIL);
        assertThat(order.items()).extracting(OrderItem::productName)
                .containsExactlyInAnyOrder("Anti-Gravity Yarn Ball", "Cosmic Milk");
        assertThat(order.total()).isEqualByComparingTo("44.48");
    }

    @Test
    void priceSnapshotIsNotAffectedByLaterPriceChange() {
        Order order = orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 2));

        yarn.update(yarn.getName(), "Yarn", new BigDecimal("99.99"), category, 100);
        productRepository.flush();

        assertThat(orderService.findById(order.id()).total()).isEqualByComparingTo("39.98");
    }

    @Test
    void placeOrderRejectsEmptyOrder() {
        assertThatThrownBy(() -> orderService.placeOrder(EMAIL, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrderRejectsNonPositiveQuantity() {
        assertThatThrownBy(() -> orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void placeOrderRejectsUnknownProduct() {
        assertThatThrownBy(() -> orderService.placeOrder(EMAIL, Map.of(987654L, 1)))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void findByIdThrowsForUnknownId() {
        assertThatThrownBy(() -> orderService.findById(987654L)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void findByOrderNumberUsesTheNaturalId() {
        Order order = orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 1));
        orderRepository.flush();

        Order found = orderService.findByOrderNumber(order.orderNumber());

        assertThat(found.id()).isEqualTo(order.id());
    }

    @Test
    void findByOrderNumberThrowsForUnknownNumber() {
        assertThatThrownBy(() -> orderService.findByOrderNumber("does-not-exist"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void updateStatusChangesTheStatus() {
        Order order = orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 1));

        Order updated = orderService.updateStatus(order.id(), OrderStatus.PAID);

        assertThat(updated.status()).isEqualTo(OrderStatus.PAID);
        assertThat(orderService.findById(order.id()).status()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void updateStatusThrowsForUnknownId() {
        assertThatThrownBy(() -> orderService.updateStatus(987654L, OrderStatus.PAID))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void deleteRemovesOrderWithItsItems() {
        Order order = orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 1));

        orderService.delete(order.id());

        assertThatThrownBy(() -> orderService.findById(order.id())).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void deleteThrowsForUnknownId() {
        assertThatThrownBy(() -> orderService.delete(987654L)).isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void bestSellingReportSumsQuantitiesAndIgnoresCancelledOrders() {
        orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 5, milk.getId(), 1));
        orderService.placeOrder(EMAIL, Map.of(milk.getId(), 3));
        Order cancelled = orderService.placeOrder(EMAIL, Map.of(milk.getId(), 100));
        orderService.updateStatus(cancelled.id(), OrderStatus.CANCELLED);

        List<ProductSales> report = orderService.findBestSellingProducts(10);

        assertThat(report).extracting(ProductSales::productName)
                .containsExactly("Anti-Gravity Yarn Ball", "Cosmic Milk");
        assertThat(report).extracting(ProductSales::totalQuantity).containsExactly(5L, 4L);
    }

    @Test
    void bestSellingReportRespectsTheLimit() {
        orderService.placeOrder(EMAIL, Map.of(yarn.getId(), 5, milk.getId(), 1));

        List<ProductSales> report = orderService.findBestSellingProducts(1);

        assertThat(report).hasSize(1);
        assertThat(report.get(0).productName()).isEqualTo("Anti-Gravity Yarn Ball");
    }
}