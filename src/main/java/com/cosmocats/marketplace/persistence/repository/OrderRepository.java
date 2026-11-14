package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.persistence.entity.OrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<OrderEntity, Long> {
}