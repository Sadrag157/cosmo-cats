package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductServiceTest {

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService();
    }

    private Product newProduct(String name) {
        return new Product(null, name, "Description", new BigDecimal("9.99"), 1L, 5);
    }

    @Test
    void startsWithThreeSeededProducts() {
        assertThat(service.count()).isEqualTo(3);
        assertThat(service.findPage(0, 10))
                .extracting(Product::name)
                .containsExactly("Anti-Gravity Yarn Ball", "Cosmic Milk", "Comet Tail Scratcher");
    }

    @Test
    void seededPricesAreExactBigDecimals() {
        assertThat(service.findById(1L).price()).isEqualByComparingTo("19.99");
        assertThat(service.findById(2L).price()).isEqualByComparingTo("4.50");
        assertThat(service.findById(3L).price()).isEqualByComparingTo("34.00");
    }

    @Test
    void createAssignsNextIdAndStoresProduct() {
        Product created = service.create(newProduct("Star Mug"));

        assertThat(created.id()).isEqualTo(4L);
        assertThat(service.count()).isEqualTo(4);
        assertThat(service.findById(4L)).isEqualTo(created);
    }

    @Test
    void createIgnoresIdProvidedByCaller() {
        Product created = service.create(new Product(99L, "Moon Rock", "Rock",
                new BigDecimal("1.00"), 1L, 1));

        assertThat(created.id()).isEqualTo(4L);
    }

    @Test
    void findPageReturnsFirstPage() {
        List<Product> page = service.findPage(0, 2);

        assertThat(page).extracting(Product::id).containsExactly(1L, 2L);
    }

    @Test
    void findPageReturnsRemainingElementsOnLastPage() {
        List<Product> page = service.findPage(1, 2);

        assertThat(page).extracting(Product::id).containsExactly(3L);
    }

    @Test
    void findPageBeyondLastPageIsEmpty() {
        assertThat(service.findPage(5, 2)).isEmpty();
    }

    @Test
    void findByIdReturnsExistingProduct() {
        assertThat(service.findById(2L).name()).isEqualTo("Cosmic Milk");
    }

    @Test
    void findByIdThrowsForUnknownId() {
        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product with id 999 was not found");
    }

    @Test
    void exceptionCarriesProductId() {
        assertThat(new ProductNotFoundException(5L).getProductId()).isEqualTo(5L);
    }

    @Test
    void updateReplacesDataAndKeepsId() {
        Product updated = service.update(2L, newProduct("Cosmic Oat Milk"));

        assertThat(updated.id()).isEqualTo(2L);
        assertThat(service.findById(2L).name()).isEqualTo("Cosmic Oat Milk");
        assertThat(service.count()).isEqualTo(3);
    }

    @Test
    void updateThrowsForUnknownId() {
        assertThatThrownBy(() -> service.update(999L, newProduct("Star Mug")))
                .isInstanceOf(ProductNotFoundException.class);
        assertThat(service.count()).isEqualTo(3);
    }

    @Test
    void deleteRemovesProduct() {
        service.delete(1L);

        assertThat(service.count()).isEqualTo(2);
        assertThatThrownBy(() -> service.findById(1L)).isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteThrowsForUnknownId() {
        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(ProductNotFoundException.class);
        assertThat(service.count()).isEqualTo(3);
    }
}