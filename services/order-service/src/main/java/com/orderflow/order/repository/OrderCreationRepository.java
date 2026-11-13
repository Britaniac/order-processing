package com.orderflow.order.repository;

import com.orderflow.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface OrderCreationRepository extends JpaRepository<Order, Long> {
    // PostgreSQL arbitrates concurrent claims; a duplicate does not abort the transaction.
    @Modifying
    @Query(value = """
            INSERT INTO orders (customer_id, request_key, status, total)
            VALUES (:customer, :key, 'PENDING', :total)
            ON CONFLICT ON CONSTRAINT orders_request_key_unique DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("customer") Long customerId, @Param("key") String key,
                       @Param("total") BigDecimal total);

    Optional<Order> findByRequestKey(String requestKey);
}
