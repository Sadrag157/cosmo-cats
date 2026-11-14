package com.cosmocats.marketplace.domain;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String reference) {
        super("Order was not found: " + reference);
    }
}