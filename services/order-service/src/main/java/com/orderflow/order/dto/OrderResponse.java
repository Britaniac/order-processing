package com.orderflow.order.dto;

import com.orderflow.order.domain.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(Long id, Long customerId, OrderStatus status,
                            BigDecimal total, List<CreateOrderRequest.Item> items) {}
