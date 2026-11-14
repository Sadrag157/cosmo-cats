package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryEntity, Long> {
}