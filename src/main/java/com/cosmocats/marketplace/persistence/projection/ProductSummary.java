package com.cosmocats.marketplace.persistence.projection;

import java.math.BigDecimal;

public interface ProductSummary {

    Long getId();

    String getName();

    BigDecimal getPrice();
}