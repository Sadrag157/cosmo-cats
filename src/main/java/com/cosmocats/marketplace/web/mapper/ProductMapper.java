package com.cosmocats.marketplace.web.mapper;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.web.dto.ProductRequest;
import com.cosmocats.marketplace.web.dto.ProductResponse;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toDto(Product product) {
        return new ProductResponse(
                product.id(),
                product.name(),
                product.description(),
                product.price(),
                product.categoryId(),
                product.stock());
    }

    public Product toDomain(ProductRequest request, Long id) {
        int stock = request.stock() == null ? 0 : request.stock();
        return new Product(
                id,
                request.name(),
                request.description(),
                request.price(),
                request.categoryId(),
                stock);
    }
}