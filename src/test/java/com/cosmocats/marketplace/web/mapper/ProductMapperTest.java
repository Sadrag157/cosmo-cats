package com.cosmocats.marketplace.web.mapper;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.web.dto.ProductRequest;
import com.cosmocats.marketplace.web.dto.ProductResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductMapperTest {

    private final ProductMapper mapper = new ProductMapper();

    @Test
    void toDtoCopiesAllFields() {
        Product product = new Product(7L, "Star Mug", "Mug", new BigDecimal("5.50"), 2L, 10);

        ProductResponse response = mapper.toDto(product);

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Star Mug");
        assertThat(response.description()).isEqualTo("Mug");
        assertThat(response.price()).isEqualByComparingTo("5.50");
        assertThat(response.categoryId()).isEqualTo(2L);
        assertThat(response.stock()).isEqualTo(10);
    }

    @Test
    void toDomainUsesGivenId() {
        ProductRequest request = new ProductRequest("Star Mug", "Mug", new BigDecimal("5.50"), 2L, 10);

        Product product = mapper.toDomain(request, 7L);

        assertThat(product.id()).isEqualTo(7L);
        assertThat(product.name()).isEqualTo("Star Mug");
        assertThat(product.price()).isEqualByComparingTo("5.50");
        assertThat(product.stock()).isEqualTo(10);
    }

    @Test
    void toDomainDefaultsMissingStockToZero() {
        ProductRequest request = new ProductRequest("Star Mug", null, new BigDecimal("5.50"), 2L, null);

        Product product = mapper.toDomain(request, null);

        assertThat(product.id()).isNull();
        assertThat(product.stock()).isZero();
    }
}