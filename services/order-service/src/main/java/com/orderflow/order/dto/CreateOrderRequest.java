package com.orderflow.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        @NotNull @Positive Long customerId,
        @NotEmpty List<@NotNull @Valid Item> items) {
    public record Item(
            @NotNull @Positive Long productId,
            @NotNull @Positive Integer quantity,
            @NotNull @DecimalMin("0") @Digits(integer = 17, fraction = 2) BigDecimal price) {}
}
