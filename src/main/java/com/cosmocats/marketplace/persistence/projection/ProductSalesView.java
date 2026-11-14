package com.cosmocats.marketplace.persistence.projection;

public interface ProductSalesView {

    Long getProductId();

    String getProductName();

    Long getTotalQuantity();
}