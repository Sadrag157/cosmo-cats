package com.cosmocats.marketplace.domain;

public class DuplicateProductException extends RuntimeException {

    private final String productName;
    private final Long categoryId;

    public DuplicateProductException(String productName, Long categoryId) {
        super("Product '" + productName + "' already exists in category " + categoryId);
        this.productName = productName;
        this.categoryId = categoryId;
    }

    public String getProductName() {
        return productName;
    }

    public Long getCategoryId() {
        return categoryId;
    }
}