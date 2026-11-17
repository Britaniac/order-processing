package com.orderflow.order.service;

import com.orderflow.order.domain.Order;
import com.orderflow.order.domain.OrderItem;
import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.repository.OrderCreationRepository;
import com.orderflow.order.repository.OrderItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCreationServiceTest {
    private static final String KEY = "order-request";
    @Mock OrderCreationRepository repository;
    @Mock OrderItemRepository itemRepository;
    @InjectMocks OrderCreationService service;
    @Captor ArgumentCaptor<List<OrderItem>> savedItems;

    @Test
    void whenKeyIsNewThenCalculateTotalAndPersistOrderItems() {
        var request = request();
        var order = order(OrderStatus.PENDING, new BigDecimal("27.50"), 123L);
        when(repository.insertIfAbsent(123L, KEY, new BigDecimal("27.50"))).thenReturn(1);
        when(repository.findByRequestKey(KEY)).thenReturn(Optional.of(order));
        when(itemRepository.findByOrderIdOrderByProductIdAsc(99L)).thenReturn(List.of(
                new OrderItem(order, 7L, 2, new BigDecimal("1.00")),
                new OrderItem(order, 42L, 3, new BigDecimal("8.50"))));

        var result = service.create(KEY, request);

        verify(itemRepository).saveAllAndFlush(savedItems.capture());

        assertTrue(result.created());
        assertEquals(new OrderResponse(99L, 123L, OrderStatus.PENDING, new BigDecimal("27.50"),
                List.of(request.items().getLast(), request.items().getFirst())), result.order());
        var items = savedItems.getValue();
        assertEquals(2, items.size());
        assertItem(items.get(0), order, 7L, 2, "1.00");
        assertItem(items.get(1), order, 42L, 3, "8.50");
    }

    @Test
    void whenKeyIsReusedThenReturnPersistedOrderWithoutSavingItems() {
        var existing = order(OrderStatus.CONFIRMED, new BigDecimal("6.00"), 456L);
        when(repository.insertIfAbsent(123L, KEY, new BigDecimal("27.50"))).thenReturn(0);
        when(repository.findByRequestKey(KEY)).thenReturn(Optional.of(existing));
        when(itemRepository.findByOrderIdOrderByProductIdAsc(99L)).thenReturn(
                List.of(new OrderItem(existing, 88L, 2, new BigDecimal("3.00"))));

        var result = service.create(KEY, request());

        assertFalse(result.created());
        assertEquals(new OrderResponse(99L, 456L, OrderStatus.CONFIRMED, new BigDecimal("6.00"),
                List.of(new CreateOrderRequest.Item(88L, 2, new BigDecimal("3.00")))), result.order());
        verify(itemRepository, never()).saveAllAndFlush(any());
    }

    @Test
    void whenProductsAreDuplicatedThenRejectBeforeAccessingRepositories() {
        var request = new CreateOrderRequest(123L, List.of(
                new CreateOrderRequest.Item(42L, 1, BigDecimal.ONE),
                new CreateOrderRequest.Item(42L, 2, BigDecimal.TEN)));

        var failure = assertThrows(ResponseStatusException.class, () -> service.create(KEY, request));

        assertEquals(HttpStatus.BAD_REQUEST, failure.getStatusCode());
        assertEquals("Duplicate product IDs are not allowed", failure.getReason());
        verifyNoInteractions(repository, itemRepository);
    }

    @Test
    void whenItemsAreFreeThenCalculateZeroTotal() {
        var request = new CreateOrderRequest(123L,
                List.of(new CreateOrderRequest.Item(42L, 3, BigDecimal.ZERO)));
        var order = order(OrderStatus.PENDING, new BigDecimal("0.00"), 123L);
        when(repository.insertIfAbsent(123L, KEY, new BigDecimal("0.00"))).thenReturn(1);
        when(repository.findByRequestKey(KEY)).thenReturn(Optional.of(order));
        when(itemRepository.findByOrderIdOrderByProductIdAsc(99L)).thenReturn(
                List.of(new OrderItem(order, 42L, 3, new BigDecimal("0.00"))));

        var result = service.create(KEY, request);

        assertTrue(result.created());
        assertEquals(new BigDecimal("0.00"), result.order().total());
        verify(repository).insertIfAbsent(123L, KEY, new BigDecimal("0.00"));
    }

    private CreateOrderRequest request() {
        return new CreateOrderRequest(123L, List.of(
                new CreateOrderRequest.Item(42L, 3, new BigDecimal("8.50")),
                new CreateOrderRequest.Item(7L, 2, new BigDecimal("1.00"))));
    }

    private Order order(OrderStatus status, BigDecimal total, Long customerId) {
        var order = new Order(status, total, customerId, KEY);
        ReflectionTestUtils.setField(order, "id", 99L);
        return order;
    }

    private void assertItem(OrderItem item, Order order, Long productId, int quantity, String price) {
        assertSame(order, item.getOrder());
        assertEquals(productId, item.getProductId());
        assertEquals(quantity, item.getQuantity());
        assertEquals(new BigDecimal(price), item.getPrice());
    }
}
