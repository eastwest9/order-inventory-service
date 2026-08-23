package com.eastwest9.orderinventory.inventory.controller;

import com.eastwest9.orderinventory.common.exception.GlobalExceptionHandler;
import com.eastwest9.orderinventory.inventory.domain.InventoryChangeType;
import com.eastwest9.orderinventory.inventory.dto.InventoryAdjustRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryCreateRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryHistoryResponseDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryReceiveRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryResponseDto;
import com.eastwest9.orderinventory.inventory.exception.DuplicateInventoryException;
import com.eastwest9.orderinventory.inventory.exception.InventoryNotFoundException;
import com.eastwest9.orderinventory.inventory.service.InventoryService;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
@Import(GlobalExceptionHandler.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @Test
    void 초기_재고_생성_성공시_201을_반환한다() throws Exception {
        given(inventoryService.createInventory(any(InventoryCreateRequestDto.class)))
                .willReturn(inventoryResponse(100));

        mockMvc.perform(
                        post("/api/inventories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "variantId": 1,
                                          "initialQuantity": 100
                                        }
                                        """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.variantId").value(1))
                .andExpect(jsonPath("$.skuCode").value("TEST-001"))
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    void 초기_재고가_음수이면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/inventories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "variantId": 1,
                                          "initialQuantity": -1
                                        }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 존재하지_않는_SKU로_초기_재고를_생성하면_404를_반환한다()
            throws Exception {
        given(inventoryService.createInventory(any(InventoryCreateRequestDto.class)))
                .willThrow(new ProductVariantNotFoundException(999L));

        mockMvc.perform(
                        post("/api/inventories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "variantId": 999,
                                          "initialQuantity": 100
                                        }
                                        """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_VARIANT_NOT_FOUND"));
    }

    @Test
    void 중복_재고를_생성하면_409를_반환한다() throws Exception {
        given(inventoryService.createInventory(any(InventoryCreateRequestDto.class)))
                .willThrow(new DuplicateInventoryException(1L));

        mockMvc.perform(
                        post("/api/inventories")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        {
                                          "variantId": 1,
                                          "initialQuantity": 100
                                        }
                                        """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_INVENTORY"));
    }

    @Test
    void 현재_재고를_조회하면_200을_반환한다() throws Exception {
        given(inventoryService.getInventory(1L))
                .willReturn(inventoryResponse(100));

        mockMvc.perform(get("/api/inventories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variantId").value(1))
                .andExpect(jsonPath("$.quantity").value(100));
    }

    @Test
    void 존재하지_않는_재고를_조회하면_404를_반환한다()
            throws Exception {
        given(inventoryService.getInventory(999L))
                .willThrow(new InventoryNotFoundException(999L));

        mockMvc.perform(get("/api/inventories/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVENTORY_NOT_FOUND"));
    }

    @Test
    void 재고_입고_성공시_200을_반환한다() throws Exception {
        given(inventoryService.receiveInventory(
                org.mockito.ArgumentMatchers.eq(1L),
                any(InventoryReceiveRequestDto.class)
        )).willReturn(inventoryResponse(130));

        mockMvc.perform(
                        post("/api/inventories/1/receipts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        { "quantity": 30 }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(130));
    }

    @Test
    void 입고_수량이_0이면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/inventories/1/receipts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        { "quantity": 0 }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 재고_조정_성공시_200을_반환한다() throws Exception {
        given(inventoryService.adjustInventory(
                org.mockito.ArgumentMatchers.eq(1L),
                any(InventoryAdjustRequestDto.class)
        )).willReturn(inventoryResponse(120));

        mockMvc.perform(
                        put("/api/inventories/1/quantity")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        { "quantity": 120 }
                                        """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(120));
    }

    @Test
    void 조정_재고가_음수이면_400을_반환한다() throws Exception {
        mockMvc.perform(
                        put("/api/inventories/1/quantity")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                        { "quantity": -1 }
                                        """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void 재고_이력을_배열로_조회한다() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        InventoryHistoryResponseDto history =
                new InventoryHistoryResponseDto(
                        20L,
                        1L,
                        InventoryChangeType.RECEIPT,
                        30,
                        100,
                        130,
                        now
                );
        given(inventoryService.getInventoryHistories(1L))
                .willReturn(List.of(history));

        mockMvc.perform(get("/api/inventories/1/histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(20))
                .andExpect(jsonPath("$[0].variantId").value(1))
                .andExpect(jsonPath("$[0].changeType").value("RECEIPT"))
                .andExpect(jsonPath("$[0].changeQuantity").value(30))
                .andExpect(jsonPath("$[0].beforeQuantity").value(100))
                .andExpect(jsonPath("$[0].afterQuantity").value(130));
    }

    private InventoryResponseDto inventoryResponse(int quantity) {
        LocalDateTime now = LocalDateTime.now();
        return new InventoryResponseDto(
                10L,
                1L,
                "TEST-001",
                quantity,
                now,
                now
        );
    }
}
