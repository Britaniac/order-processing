package com.orderflow.order.service;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.repository.OrderCreationRepository;
import com.orderflow.order.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderCreationService {
    private final OrderCreationRepository repository;
    private final OrderItemRepository itemRepository;

    @Transactional
    public Result create(String key, CreateOrderRequest request) {
        List<CreateOrderRequest.Item> items = request.items().stream()
                .sorted(Comparator.comparing(CreateOrderRequest.Item::productId)).toList();
        if (items.stream().map(CreateOrderRequest.Item::productId).distinct().count() != items.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Duplicate product IDs are not allowed");
        }
        BigDecimal total = items.stream().map(i -> i.price().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);

        // The UI assigns a new key per order and reuses it for retries.
        boolean created = repository.insertIfAbsent(request.customerId(), key, total) == 1;
        // A separate statement gets a fresh READ COMMITTED snapshot after any conflict wait.
        Order order = repository.findByRequestKey(key).orElseThrow();
        if (created) {
            itemRepository.saveAllAndFlush(items.stream()
                    .map(item -> new OrderItem(order, item.productId(), item.quantity(), item.price())).toList());
        }
        List<CreateOrderRequest.Item> responseItems = itemRepository.findByOrderIdOrderByProductIdAsc(order.getId()).stream()
                .map(item -> new CreateOrderRequest.Item(item.getProductId(), item.getQuantity(), item.getPrice()))
                .toList();
        return new Result(created, new OrderResponse(order.getId(), order.getCustomerId(), order.getStatus(),
                order.getTotal(), responseItems));
    }

    public record Result(boolean created, OrderResponse order) {}
}
