package com.cosmocats.marketplace.domain;

public class CategoryNotFoundException extends RuntimeException {

    private final Long categoryId;

    public CategoryNotFoundException(Long categoryId) {
        super("Category with id " + categoryId + " was not found");
        this.categoryId = categoryId;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}