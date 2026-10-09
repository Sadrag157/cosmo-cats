package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.service.ProductService;
import com.cosmocats.marketplace.web.dto.PageResponse;
import com.cosmocats.marketplace.web.dto.ProductRequest;
import com.cosmocats.marketplace.web.dto.ProductResponse;
import com.cosmocats.marketplace.web.mapper.ProductMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService, ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @GetMapping
    public PageResponse<ProductResponse> list(
            @RequestParam(name = "page", defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(100) int size) {
        List<ProductResponse> content = productService.findPage(page, size).stream()
                .map(productMapper::toDto)
                .toList();
        long totalElements = productService.count();
        int totalPages = (int) ((totalElements + size - 1) / size);
        return new PageResponse<>(content, page, size, totalElements, totalPages);
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest request) {
        Product created = productService.create(productMapper.toDomain(request, null));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(productMapper.toDto(created));
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable("id") @Positive Long id) {
        return productMapper.toDto(productService.findById(id));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable("id") @Positive Long id, @Valid @RequestBody ProductRequest request) {
        Product updated = productService.update(id, productMapper.toDomain(request, id));
        return productMapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") @Positive Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}