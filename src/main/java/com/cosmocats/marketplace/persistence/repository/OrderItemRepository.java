package com.cosmocats.marketplace.persistence.repository;

import com.cosmocats.marketplace.domain.OrderStatus;
import com.cosmocats.marketplace.persistence.entity.OrderItemEntity;
import com.cosmocats.marketplace.persistence.projection.ProductSalesView;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItemEntity, Long> {

    @Query("""
            select p.id as productId, p.name as productName, sum(oi.quantity) as totalQuantity
            from OrderItemEntity oi
            join oi.product p
            join oi.purchaseOrder o
            where o.status <> :excludedStatus
            group by p.id, p.name
            order by sum(oi.quantity) desc, p.name asc
            """)
    List<ProductSalesView> findBestSellingProducts(@Param("excludedStatus") OrderStatus excludedStatus,
                                                   Pageable pageable);
}