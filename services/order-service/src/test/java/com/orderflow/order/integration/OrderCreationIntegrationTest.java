package com.orderflow.order.integration;

import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.CreateOrderRequest;
import com.orderflow.order.service.OrderCreationService;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class OrderCreationIntegrationTest {
    @Autowired OrderCreationService service;
    @Autowired JdbcTemplate jdbc;

    @BeforeEach
    void clean() {
        jdbc.execute("TRUNCATE order_item, orders RESTART IDENTITY");
    }

    @Test
    void whenOrderIsCreatedAndRetriedThenPersistCompleteOrderOnce() {
        var created = service.create("create", request(123L));
        assertTrue(created.created());
        var order = created.order();
        assertEquals(123L, order.customerId());
        assertEquals(OrderStatus.PENDING, order.status());
        assertEquals(new BigDecimal("27.50"), order.total());
        assertEquals(List.of(
                new CreateOrderRequest.Item(7L, 2, new BigDecimal("1.00")),
                new CreateOrderRequest.Item(42L, 3, new BigDecimal("8.50"))), order.items());
        assertEquals(new BigDecimal("27.50"), jdbc.queryForObject("SELECT total FROM orders", BigDecimal.class));
        assertEquals(2, count("order_item"));
        assertNotNull(jdbc.queryForObject("SELECT created_at FROM orders", java.time.OffsetDateTime.class));
        var updatedAt = jdbc.queryForObject("SELECT updated_at FROM orders", java.time.OffsetDateTime.class);
        assertNotNull(updatedAt);

        var replay = service.create("create", new CreateOrderRequest(123L, List.of(
                new CreateOrderRequest.Item(7L, 2, new BigDecimal("1")),
                new CreateOrderRequest.Item(42L, 3, new BigDecimal("8.5")))));
        assertFalse(replay.created());
        assertEquals(order, replay.order());
        assertEquals(1, count("orders"));
        assertEquals(updatedAt, jdbc.queryForObject("SELECT updated_at FROM orders", java.time.OffsetDateTime.class));

        jdbc.update("UPDATE orders SET status = 'CONFIRMED'");
        var confirmedReplay = service.create("create", request(123L));
        assertFalse(confirmedReplay.created());
        assertEquals(order.id(), confirmedReplay.order().id());
        assertEquals(OrderStatus.CONFIRMED, confirmedReplay.order().status());
    }
    @Test
    void whenKeysDifferThenPersistDistinctOrders() {
        var first = service.create("first-order", request(123L));
        var second = service.create("second-order", request(123L));
        assertTrue(first.created());
        assertTrue(second.created());
        assertNotEquals(first.order().id(), second.order().id());
        assertEquals(2, count("orders"));
        assertEquals(4, count("order_item"));
    }

    @Test
    void whenIdenticalRequestsRunConcurrentlyThenPersistExactlyOneOrder() throws Exception {
        var responses = concurrently(java.util.Collections.nCopies(8, request(123L)));
        assertEquals(1, responses.stream().filter(OrderCreationService.Result::created).count());
        assertEquals(7, responses.stream().filter(r -> !r.created()).count());
        for (var response : responses) {
            assertEquals(responses.getFirst().order(), response.order());
        }
        assertEquals(1, count("orders"));
        assertEquals(2, count("order_item"));
    }

    @Test
    void whenDifferentPayloadsShareKeyConcurrentlyThenReturnSameOrder() throws Exception {
        var responses = concurrently(List.of(request(123L), request(456L)));
        assertEquals(1, responses.stream().filter(OrderCreationService.Result::created).count());
        assertEquals(responses.getFirst().order(), responses.getLast().order());
        assertEquals(1, count("orders"));
        assertEquals(2, count("order_item"));
    }

    @Test
    void whenKeyOwnerCommitsThenWaitingRequestReadsCommittedOrder() throws Exception {
        verifyWaitingContender(true);
    }

    @Test
    void whenKeyOwnerRollsBackThenWaitingRequestCreatesOrder() throws Exception {
        verifyWaitingContender(false);
    }

    private void verifyWaitingContender(boolean commit) throws Exception {
        try (var connection = jdbc.getDataSource().getConnection();
             var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            connection.setAutoCommit(false);
            try {
                try (var statement = connection.createStatement()) {
                    statement.executeUpdate("""
                            INSERT INTO orders (customer_id, request_key, status, total)
                            VALUES (123, 'concurrent', 'PENDING', 27.50)
                            """);
                    statement.executeUpdate("""
                            INSERT INTO order_item (order_id, product_id, quantity, price)
                            SELECT id, 7, 2, 1.00 FROM orders WHERE request_key = 'concurrent'
                            """);
                    statement.executeUpdate("""
                            INSERT INTO order_item (order_id, product_id, quantity, price)
                            SELECT id, 42, 3, 8.50 FROM orders WHERE request_key = 'concurrent'
                            """);
                }
                var contender = executor.submit(() -> service.create("concurrent", request(123L)));
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                boolean waiting = false;
                while (System.nanoTime() < deadline) {
                    waiting = Boolean.TRUE.equals(jdbc.queryForObject("""
                            SELECT EXISTS (SELECT 1 FROM pg_stat_activity
                            WHERE datname = current_database() AND wait_event_type = 'Lock'
                              AND query LIKE '%ON CONFLICT ON CONSTRAINT orders_request_key_unique%')
                            """, Boolean.class));
                    if (waiting) break;
                    Thread.sleep(25);
                }
                assertTrue(waiting, "Contender must wait on the uncommitted unique key");
                assertFalse(contender.isDone());
                if (commit) connection.commit();
                else connection.rollback();
                var response = contender.get(10, TimeUnit.SECONDS);
                assertEquals(!commit, response.created());
                assertEquals(1, count("orders"));
                assertEquals(2, count("order_item"));
            } finally {
                connection.rollback();
            }
        }
    }

    @Test
    void whenItemPersistenceFailsThenRollBackOrderAndReleaseKey() {
        // Fail the second sorted item after the first item and parent have been inserted.
        jdbc.execute("ALTER TABLE order_item ADD CONSTRAINT test_item_failure CHECK (product_id <> 42)");
        try {
            assertThrows(DataIntegrityViolationException.class, () -> service.create("rollback", request(123L)));
            assertEquals(0, count("orders"));
            assertEquals(0, count("order_item"));
        } finally {
            jdbc.execute("ALTER TABLE order_item DROP CONSTRAINT test_item_failure");
        }
        assertTrue(service.create("rollback", request(123L)).created());
        assertEquals(1, count("orders"));
        assertEquals(2, count("order_item"));
    }

    private List<OrderCreationService.Result> concurrently(List<CreateOrderRequest> requests) throws Exception {
        var ready = new CountDownLatch(requests.size());
        var start = new CountDownLatch(1);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new ArrayList<Future<OrderCreationService.Result>>();
            for (var request : requests) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    assertTrue(start.await(10, TimeUnit.SECONDS));
                    return service.create("concurrent", request);
                }));
            }
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var responses = new ArrayList<OrderCreationService.Result>();
            for (var future : futures) responses.add(future.get(30, TimeUnit.SECONDS));
            return responses;
        }
    }

    private CreateOrderRequest request(Long customerId) {
        return new CreateOrderRequest(customerId, List.of(
                new CreateOrderRequest.Item(42L, 3, new BigDecimal("8.50")),
                new CreateOrderRequest.Item(7L, 2, new BigDecimal("1.00"))));
    }
    private int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DatabaseConfiguration {
        @Bean
        @ServiceConnection
        PostgreSQLContainer postgres() {
            return new PostgreSQLContainer("postgres:17");
        }
    }
}
