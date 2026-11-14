package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.DuplicateProductException;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import com.cosmocats.marketplace.persistence.repository.ProductRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Product create(Product product) {
        CategoryEntity category = loadCategory(product.categoryId());
        if (productRepository.existsByCategoryIdAndName(product.categoryId(), product.name())) {
            throw new DuplicateProductException(product.name(), product.categoryId());
        }
        ProductEntity entity = new ProductEntity(product.name(), product.description(), product.price(),
                category, product.stock());
        return productRepository.save(entity).toDomain();
    }

    @Transactional(readOnly = true)
    public List<Product> findPage(int page, int size) {
        return productRepository.findAll(PageRequest.of(page, size, Sort.by("id"))).stream()
                .map(ProductEntity::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public long count() {
        return productRepository.count();
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return loadProduct(id).toDomain();
    }

    @Transactional
    public Product update(Long id, Product product) {
        ProductEntity entity = loadProduct(id);
        CategoryEntity category = loadCategory(product.categoryId());
        if (productRepository.existsByCategoryIdAndNameAndIdNot(product.categoryId(), product.name(), id)) {
            throw new DuplicateProductException(product.name(), product.categoryId());
        }
        entity.update(product.name(), product.description(), product.price(), category, product.stock());
        return entity.toDomain();
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    private ProductEntity loadProduct(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new ProductNotFoundException(id));
    }

    private CategoryEntity loadCategory(Long categoryId) {
        return categoryRepository.findById(categoryId).orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }
}