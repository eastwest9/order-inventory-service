package com.eastwest9.orderinventory.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.eastwest9.orderinventory.order.dto.OrderCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderResponseDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SynchronizedOrderServiceTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private SynchronizedOrderService synchronizedOrderService;

    @Test
    void 주문_생성을_OrderService에_한_번_위임한다() {
        OrderCreateRequestDto request = org.mockito.Mockito.mock(OrderCreateRequestDto.class);
        OrderResponseDto response = org.mockito.Mockito.mock(OrderResponseDto.class);
        given(orderService.createOrder(request)).willReturn(response);

        synchronizedOrderService.createOrder(request);

        verify(orderService).createOrder(request);
    }

    @Test
    void OrderService의_응답을_그대로_반환한다() {
        OrderCreateRequestDto request = org.mockito.Mockito.mock(OrderCreateRequestDto.class);
        OrderResponseDto response = org.mockito.Mockito.mock(OrderResponseDto.class);
        given(orderService.createOrder(request)).willReturn(response);

        OrderResponseDto result = synchronizedOrderService.createOrder(request);

        assertThat(result).isSameAs(response);
    }

    @Test
    void OrderService의_RuntimeException을_그대로_전파한다() {
        OrderCreateRequestDto request = org.mockito.Mockito.mock(OrderCreateRequestDto.class);
        RuntimeException exception = new RuntimeException("주문 생성 실패");
        given(orderService.createOrder(request)).willThrow(exception);

        assertThatThrownBy(() -> synchronizedOrderService.createOrder(request))
                .isSameAs(exception);
    }
}
