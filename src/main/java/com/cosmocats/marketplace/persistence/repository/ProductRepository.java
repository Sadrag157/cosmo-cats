package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.projection.ProductSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

    boolean existsByCategoryIdAndName(Long categoryId, String name);

    boolean existsByCategoryIdAndNameAndIdNot(Long categoryId, String name, Long id);

    List<ProductSummary> findByCategoryIdOrderByPriceAsc(Long categoryId);
}