package com.orderflow.order.service;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.repository.OrderCreationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderQueryServiceTest {
    @Mock OrderCreationRepository orders;
    @InjectMocks OrderQueryService service;

    @Test
    void whenOrderExistsThenMapOrderAndItems() {
        Order order = new Order(OrderStatus.PENDING, new BigDecimal("6.00"), 123L, "first");
        order.setId(1L);
        order.getItems().addAll(List.of(
                new OrderItem(order, 7L, 2, new BigDecimal("1.50")),
                new OrderItem(order, 42L, 1, new BigDecimal("3.00"))));
        OrderResponse expected = new OrderResponse(1L, 123L, OrderStatus.PENDING,
                new BigDecimal("6.00"), List.of(
                new CreateOrderRequest.Item(7L, 2, new BigDecimal("1.50")),
                new CreateOrderRequest.Item(42L, 1, new BigDecimal("3.00"))));

        when(orders.findWithItemsById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = service.get(1L);

        verify(orders).findWithItemsById(1L);

        assertEquals(expected, response);
    }

    @Test
    void whenOrderIsMissingThenThrowNotFound() {
        when(orders.findWithItemsById(1L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> service.get(1L));

        verify(orders).findWithItemsById(1L);

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Order not found", exception.getReason());
    }

    @Test
    void whenFetchedOrdersAreUnorderedThenPreservePageOrder() {
        Order first = new Order(OrderStatus.PENDING, BigDecimal.ZERO, 123L, "first");
        first.setId(1L);
        Order second = new Order(OrderStatus.PENDING, BigDecimal.ZERO, 123L, "second");
        second.setId(2L);
        PageRequest request = PageRequest.of(0, 2, Sort.by("id"));
        Page<Long> ids = new PageImpl<>(List.of(1L, 2L), request, 3);

        when(orders.findIds(request)).thenReturn(ids);
        when(orders.findWithItemsByIdIn(List.of(1L, 2L))).thenReturn(List.of(second, first));

        Page<OrderResponse> response = service.list(request);

        verify(orders).findIds(request);
        verify(orders).findWithItemsByIdIn(List.of(1L, 2L));

        assertEquals(List.of(1L, 2L), response.getContent().stream().map(OrderResponse::id).toList());
    }

    @Test
    void whenPageContainsOrderThenMapOrderAndItems() {
        Order order = new Order(OrderStatus.PENDING, new BigDecimal("6.00"), 123L, "first");
        order.setId(1L);
        order.getItems().add(new OrderItem(order, 7L, 2, new BigDecimal("3.00")));
        PageRequest request = PageRequest.of(0, 2, Sort.by("id"));
        Page<Long> ids = new PageImpl<>(List.of(1L), request, 1);
        OrderResponse expected = new OrderResponse(1L, 123L, OrderStatus.PENDING,
                new BigDecimal("6.00"), List.of(new CreateOrderRequest.Item(7L, 2, new BigDecimal("3.00"))));

        when(orders.findIds(request)).thenReturn(ids);
        when(orders.findWithItemsByIdIn(List.of(1L))).thenReturn(List.of(order));

        Page<OrderResponse> response = service.list(request);

        verify(orders).findIds(request);
        verify(orders).findWithItemsByIdIn(List.of(1L));

        assertEquals(List.of(expected), response.getContent());
    }

    @Test
    void whenPageIsEmptyThenSkipItemQuery() {
        PageRequest request = PageRequest.of(0, 20, Sort.by("id"));
        Page<Long> ids = Page.empty(request);

        when(orders.findIds(request)).thenReturn(ids);

        Page<OrderResponse> response = service.list(request);

        verify(orders).findIds(request);
        verify(orders, never()).findWithItemsByIdIn(anyList());

        assertTrue(response.isEmpty());
        assertEquals(request, response.getPageable());
        assertEquals(0, response.getTotalElements());
    }

    @Test
    void whenRequestingLastPageThenPreservePaginationAndTotal() {
        PageRequest request = PageRequest.of(1, 2, Sort.by("id"));
        Order third = new Order(OrderStatus.PENDING, BigDecimal.ZERO, 123L, "third");
        third.setId(3L);
        Page<Long> ids = new PageImpl<>(List.of(3L), request, 3);

        when(orders.findIds(request)).thenReturn(ids);
        when(orders.findWithItemsByIdIn(List.of(3L))).thenReturn(List.of(third));

        Page<OrderResponse> response = service.list(request);

        verify(orders).findIds(request);
        verify(orders).findWithItemsByIdIn(List.of(3L));

        assertEquals(request, response.getPageable());
        assertEquals(3, response.getTotalElements());
    }

    @Test
    void whenPageIsBeyondLastPageThenPreserveTotalWithoutFetchingOrders() {
        PageRequest request = PageRequest.of(2, 2, Sort.by("id"));
        Page<Long> ids = new PageImpl<>(List.of(), request, 3);

        when(orders.findIds(request)).thenReturn(ids);

        Page<OrderResponse> response = service.list(request);

        verify(orders).findIds(request);
        verify(orders, never()).findWithItemsByIdIn(anyList());

        assertTrue(response.isEmpty());
        assertEquals(request, response.getPageable());
        assertEquals(3, response.getTotalElements());
    }
}
