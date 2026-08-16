package com.eastwest9.orderinventory.product.controller;

import com.eastwest9.orderinventory.common.exception.GlobalExceptionHandler;
import com.eastwest9.orderinventory.product.domain.ProductStatus;
import com.eastwest9.orderinventory.product.dto.ProductCreateRequestDto;
import com.eastwest9.orderinventory.product.dto.ProductResponseDto;
import com.eastwest9.orderinventory.product.exception.ProductNotFoundException;
import com.eastwest9.orderinventory.product.service.ProductService;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(GlobalExceptionHandler.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @Test
    void 상품_등록_성공시_201을_반환한다() throws Exception {
        // given
        LocalDateTime now = LocalDateTime.now();

        ProductResponseDto response =
                new ProductResponseDto(
                        1L,
                        "테스트 상품",
                        ProductStatus.ON_SALE,
                        List.of(),
                        now,
                        now
                );

        given(productService.createProduct(
                any(ProductCreateRequestDto.class)
        )).willReturn(response);

        String request = """
                {
                  "name": "테스트 상품",
                  "status": "ON_SALE",
                  "variants": [
                    {
                      "skuCode": "TEST-100",
                      "name": "기본 옵션",
                      "salePrice": 10000,
                      "status": "ON_SALE"
                    }
                  ]
                }
                """;

        // when & then
        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("테스트 상품"))
                .andExpect(jsonPath("$.status").value("ON_SALE"));
    }

    @Test
    void 상품명이_비어있으면_400을_반환한다() throws Exception {
        String request = """
                {
                  "name": "",
                  "status": "ON_SALE",
                  "variants": [
                    {
                      "skuCode": "TEST-101",
                      "name": "기본 옵션",
                      "salePrice": 10000,
                      "status": "ON_SALE"
                    }
                  ]
                }
                """;

        mockMvc.perform(
                        post("/api/products")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(request)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message")
                        .value("name: 상품명은 필수입니다."));
    }

    @Test
    void 존재하지_않는_상품을_조회하면_404를_반환한다()
            throws Exception {

        // given
        given(productService.getProduct(999L))
                .willThrow(new ProductNotFoundException(999L));

        // when & then
        mockMvc.perform(
                        get("/api/products/999")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message")
                        .value(
                                "상품을 찾을 수 없습니다. productId=999"
                        ));
    }
}