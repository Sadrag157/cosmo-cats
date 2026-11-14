package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.CategoryNotFoundException;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public Category create(Category category) {
        return categoryRepository.save(new CategoryEntity(category.name(), category.description())).toDomain();
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAll(Sort.by("id")).stream()
                .map(CategoryEntity::toDomain)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return load(id).toDomain();
    }

    @Transactional
    public Category update(Long id, Category category) {
        CategoryEntity entity = load(id);
        entity.update(category.name(), category.description());
        return entity.toDomain();
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
    }

    private CategoryEntity load(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }
}