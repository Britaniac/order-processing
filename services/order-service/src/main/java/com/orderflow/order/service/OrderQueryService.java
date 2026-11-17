package com.orderflow.order.service;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.repository.OrderCreationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {
    private final OrderCreationRepository orders;

    public OrderResponse get(Long id) {
        Order order = orders.findWithItemsById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return response(order, order.getItems());
    }

    public Page<OrderResponse> list(Pageable pageable) {
        Page<Long> ids = orders.findIds(pageable);
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, ids.getTotalElements());
        }
        Map<Long, Order> fetched = orders.findWithItemsByIdIn(ids.getContent()).stream()
                .collect(Collectors.toMap(Order::getId, order -> order));
        // The IN query does not guarantee the order selected by Pageable.
        return ids.map(id -> {
            Order order = fetched.get(id);
            return response(order, order.getItems());
        });
    }

    private OrderResponse response(Order order, List<OrderItem> orderItems) {
        return new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus(), order.getTotal(),
                orderItems.stream().map(item -> new CreateOrderRequest.Item(
                        item.getProductId(), item.getQuantity(), item.getPrice())).toList());
    }
}
