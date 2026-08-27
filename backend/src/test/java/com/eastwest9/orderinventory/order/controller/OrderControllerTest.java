package com.eastwest9.orderinventory.order.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eastwest9.orderinventory.common.exception.GlobalExceptionHandler;
import com.eastwest9.orderinventory.inventory.exception.InsufficientInventoryException;
import com.eastwest9.orderinventory.member.exception.MemberNotFoundException;
import com.eastwest9.orderinventory.order.domain.OrderStatus;
import com.eastwest9.orderinventory.order.dto.OrderCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderItemResponseDto;
import com.eastwest9.orderinventory.order.dto.OrderResponseDto;
import com.eastwest9.orderinventory.order.dto.OrderSummaryResponseDto;
import com.eastwest9.orderinventory.order.exception.InvalidOrderException;
import com.eastwest9.orderinventory.order.exception.OrderAlreadyCanceledException;
import com.eastwest9.orderinventory.order.exception.OrderNotFoundException;
import com.eastwest9.orderinventory.order.service.OrderService;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotFoundException;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotOrderableException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
@Import(GlobalExceptionHandler.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Test
    void 주문_생성_성공시_201을_반환한다() throws Exception {
        given(orderService.createOrder(any(OrderCreateRequestDto.class)))
                .willReturn(orderResponse(OrderStatus.CREATED, null));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateRequest())
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.orderNumber").value("ORD-TEST"))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.totalAmount").value(20000));
    }

    @Test
    void memberId가_null이면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "memberId": null,
                                          "items": [{"variantId": 10, "quantity": 2}]
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 주문_항목이_비어있으면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {"memberId": 1, "items": []}
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 주문_수량이_0이면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "memberId": 1,
                                          "items": [{"variantId": 10, "quantity": 0}]
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 회원이_없으면_404를_반환한다() throws Exception {
        given(orderService.createOrder(any(OrderCreateRequestDto.class)))
                .willThrow(new MemberNotFoundException(999L));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateRequest())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    void 상품_SKU가_없으면_404를_반환한다() throws Exception {
        given(orderService.createOrder(any(OrderCreateRequestDto.class)))
                .willThrow(new ProductVariantNotFoundException(10L));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateRequest())
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_VARIANT_NOT_FOUND"));
    }

    @Test
    void 판매_불가능한_SKU면_409를_반환한다() throws Exception {
        given(orderService.createOrder(any(OrderCreateRequestDto.class)))
                .willThrow(new ProductVariantNotOrderableException(10L));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateRequest())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_VARIANT_NOT_ORDERABLE"));
    }

    @Test
    void 재고가_부족하면_409를_반환한다() throws Exception {
        given(orderService.createOrder(any(OrderCreateRequestDto.class)))
                .willThrow(new InsufficientInventoryException(10L, 2, 1));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateRequest())
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("INSUFFICIENT_INVENTORY"));
    }

    @Test
    void 잘못된_주문이면_400을_반환한다() throws Exception {
        given(orderService.createOrder(any(OrderCreateRequestDto.class)))
                .willThrow(new InvalidOrderException("중복 SKU"));

        mockMvc.perform(
                        post("/api/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(validCreateRequest())
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ORDER"));
    }

    @Test
    void 주문을_단건_조회하면_항목을_포함해_200을_반환한다()
            throws Exception {
        given(orderService.getOrder(1L))
                .willReturn(orderResponse(OrderStatus.CREATED, null));

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.items[0].variantId").value(10))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
    }

    @Test
    void 주문이_없으면_404를_반환한다() throws Exception {
        given(orderService.getOrder(999L))
                .willThrow(new OrderNotFoundException(999L));

        mockMvc.perform(get("/api/orders/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void 주문_목록은_Summary_배열로_반환한다() throws Exception {
        given(orderService.getOrders()).willReturn(List.of(orderSummary()));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].orderNumber").value("ORD-TEST"))
                .andExpect(jsonPath("$[0].itemCount").value(1))
                .andExpect(jsonPath("$[0].items").doesNotExist());
    }

    @Test
    void 주문_취소_성공시_200을_반환한다() throws Exception {
        LocalDateTime canceledAt = LocalDateTime.of(2026, 8, 26, 12, 0);
        given(orderService.cancelOrder(1L))
                .willReturn(orderResponse(OrderStatus.CANCELED, canceledAt));

        mockMvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.canceledAt").exists());
    }

    @Test
    void 이미_취소된_주문이면_409를_반환한다() throws Exception {
        given(orderService.cancelOrder(1L))
                .willThrow(new OrderAlreadyCanceledException());

        mockMvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("ORDER_ALREADY_CANCELED"));
    }

    private String validCreateRequest() {
        return """
                {
                  "memberId": 1,
                  "items": [{"variantId": 10, "quantity": 2}]
                }
                """;
    }

    private OrderResponseDto orderResponse(
            OrderStatus status,
            LocalDateTime canceledAt
    ) {
        LocalDateTime now = LocalDateTime.of(2026, 8, 26, 10, 0);
        OrderItemResponseDto item = new OrderItemResponseDto(
                100L,
                10L,
                "테스트 상품",
                "기본 옵션",
                new BigDecimal("10000"),
                2,
                new BigDecimal("20000")
        );
        return new OrderResponseDto(
                1L,
                "ORD-TEST",
                1L,
                status,
                new BigDecimal("20000"),
                canceledAt,
                now,
                now,
                List.of(item)
        );
    }

    private OrderSummaryResponseDto orderSummary() {
        return new OrderSummaryResponseDto(
                1L,
                "ORD-TEST",
                1L,
                OrderStatus.CREATED,
                new BigDecimal("20000"),
                1,
                null,
                LocalDateTime.of(2026, 8, 26, 10, 0)
        );
    }
}
