package com.orderflow.order.controller;

import com.orderflow.order.domain.OrderStatus;
import com.orderflow.order.dto.OrderResponse;
import com.orderflow.order.service.OrderQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderQueryControllerTest {
    @Mock OrderQueryService service;
    @InjectMocks OrderQueryController controller;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void whenGettingOrderThenInvokeServiceAndReturnOk() throws Exception {
        OrderResponse response = new OrderResponse(1L, 123L, OrderStatus.PENDING,
                new BigDecimal("6.00"), List.of());
        when(service.get(1L)).thenReturn(response);

        mvc.perform(get("/api/orders/1")).andExpect(status().isOk());

        verify(service).get(1L);
    }

    @Test
    void whenListingOrdersThenForwardPaginationAndReturnOk() throws Exception {
        PageRequest request = PageRequest.of(2, 10, Sort.by("id"));
        when(service.list(request)).thenReturn(Page.empty(request));

        mvc.perform(get("/api/orders").param("page", "2").param("size", "10"))
                .andExpect(status().isOk());

        verify(service).list(request);
    }

    @Test
    void whenPaginationIsOmittedThenUseDefaultsAndReturnOk() throws Exception {
        PageRequest request = PageRequest.of(0, 20, Sort.by("id"));
        when(service.list(request)).thenReturn(Page.empty(request));

        mvc.perform(get("/api/orders")).andExpect(status().isOk());

        verify(service).list(request);
    }

    @Test
    void whenSizeIs101ThenForwardRequestedSizeAndReturnOk() throws Exception {
        PageRequest request = PageRequest.of(0, 101, Sort.by("id"));
        when(service.list(request)).thenReturn(Page.empty(request));

        mvc.perform(get("/api/orders").param("size", "101")).andExpect(status().isOk());

        verify(service).list(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/orders?page=-1", "/api/orders?page=abc",
            "/api/orders?size=0", "/api/orders?size=abc"})
    void whenPaginationIsInvalidThenUseDefaultsAndReturnOk(String uri) throws Exception {
        PageRequest request = PageRequest.of(0, 20, Sort.by("id"));
        when(service.list(request)).thenReturn(Page.empty(request));

        mvc.perform(get(uri)).andExpect(status().isOk());

        verify(service).list(request);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/orders/0", "/api/orders/-1", "/api/orders/abc"})
    void whenOrderIdIsInvalidThenReturnBadRequestWithoutInvokingService(String uri) throws Exception {
        mvc.perform(get(uri)).andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
