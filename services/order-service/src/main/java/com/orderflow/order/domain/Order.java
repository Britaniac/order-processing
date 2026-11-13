package com.orderflow.order.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal total;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "request_key", nullable = false, length = 128)
    private String requestKey;

    public Order(OrderStatus status, BigDecimal total, Long customerId,
                 String requestKey) {
        this.status = status;
        this.total = total;
        this.customerId = customerId;
        this.requestKey = requestKey;
    }
}
