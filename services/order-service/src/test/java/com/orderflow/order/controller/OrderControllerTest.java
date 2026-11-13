package com.orderflow.order.controller;

import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.service.OrderCreationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {
    private static final String KEY = "order-request";
    @Mock OrderCreationService service;
    @InjectMocks OrderController controller;

    @Test
    void whenOrderIsCreatedThenReturnCreatedWithServiceResponse() {
        var request = request();
        var response = new OrderResponse(99L, 123L, OrderStatus.PENDING,
                new BigDecimal("25.50"), request.items());
        when(service.create(KEY, request)).thenReturn(new OrderCreationService.Result(true, response));

        var result = controller.create(KEY, request);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).create(KEY, request);
        verifyNoMoreInteractions(service);
    }

    @Test
    void whenKeyIsReusedThenReturnOkWithPersistedOrder() {
        var request = request();
        var response = new OrderResponse(99L, 456L, OrderStatus.CONFIRMED, new BigDecimal("6.00"),
                List.of(new CreateOrderRequest.Item(7L, 2, new BigDecimal("3.00"))));
        when(service.create(KEY, request)).thenReturn(new OrderCreationService.Result(false, response));

        var result = controller.create(KEY, request);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertSame(response, result.getBody());
        verify(service).create(KEY, request);
        verifyNoMoreInteractions(service);
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(123L,
                List.of(new CreateOrderRequest.Item(42L, 3, new BigDecimal("8.50"))));
    }
}
