package com.cosmocats.marketplace.domain;

import java.math.BigDecimal;


public record Product(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        int stock) { }