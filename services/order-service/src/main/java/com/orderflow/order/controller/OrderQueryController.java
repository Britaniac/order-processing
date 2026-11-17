package com.orderflow.order.controller;

import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.service.OrderQueryService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/orders")
public class OrderQueryController {
    private final OrderQueryService service;

    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable @Positive Long id) {
        return service.get(id);
    }

    @GetMapping
    public Page<OrderResponse> list(@PageableDefault(size = 20, sort = "id") Pageable pageable) {
        return service.list(pageable);
    }
}
