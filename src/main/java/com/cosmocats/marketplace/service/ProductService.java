package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ProductService {

    private final Map<Long, Product> products = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    public ProductService() {
        create(new Product(null, "Anti-Gravity Yarn Ball",
                "A weightless yarn ball that floats around the living room.",
                new BigDecimal("19.99"), 1L, 120));
        create(new Product(null, "Cosmic Milk",
                "Creamy milk with a faint galactic glow.",
                new BigDecimal("4.50"), 2L, 300));
        create(new Product(null, "Comet Tail Scratcher",
                "A scratching post made from a real comet tail.",
                new BigDecimal("34.00"), 1L, 45));
    }

    public Product create(Product product) {
        Product stored = product.withId(idSequence.incrementAndGet());
        products.put(stored.id(), stored);
        return stored;
    }

    public List<Product> findPage(int page, int size) {
        long offset = (long) page * size;
        return products.values().stream()
                .sorted(Comparator.comparing(Product::id))
                .skip(offset)
                .limit(size)
                .toList();
    }

    public long count() {
        return products.size();
    }

    public Product findById(Long id) {
        Product product = products.get(id);
        if (product == null) {
            throw new ProductNotFoundException(id);
        }
        return product;
    }

    public Product update(Long id, Product product) {
        Product updated = product.withId(id);
        if (products.replace(id, updated) == null) {
            throw new ProductNotFoundException(id);
        }
        return updated;
    }

    public void delete(Long id) {
        if (products.remove(id) == null) {
            throw new ProductNotFoundException(id);
        }
    }
}