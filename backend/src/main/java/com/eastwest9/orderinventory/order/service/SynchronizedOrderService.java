package com.eastwest9.orderinventory.order.service;

import com.eastwest9.orderinventory.order.dto.OrderCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SynchronizedOrderService {

    private final OrderService orderService;

    public synchronized OrderResponseDto createOrder(OrderCreateRequestDto request) {
        return orderService.createOrder(request);
    }
}
