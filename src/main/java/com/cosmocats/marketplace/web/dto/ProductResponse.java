package com.cosmocats.marketplace.web.dto;

import java.math.BigDecimal;


public record ProductResponse(
        Long id,
        String name,
        String description,
        BigDecimal price,
        Long categoryId,
        int stock) {
}