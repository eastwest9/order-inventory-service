package com.eastwest9.orderinventory.product.service;

import com.eastwest9.orderinventory.product.domain.Product;
import com.eastwest9.orderinventory.product.domain.ProductStatus;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import com.eastwest9.orderinventory.product.dto.ProductCreateRequestDto;
import com.eastwest9.orderinventory.product.dto.ProductResponseDto;
import com.eastwest9.orderinventory.product.dto.ProductVariantCreateRequestDto;
import com.eastwest9.orderinventory.product.exception.DuplicateSkuCodeException;
import com.eastwest9.orderinventory.product.exception.ProductNotFoundException;
import com.eastwest9.orderinventory.product.repository.ProductRepository;
import com.eastwest9.orderinventory.product.repository.ProductVariantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void 상품을_등록한다() {
        // given
        ProductVariantCreateRequestDto variantRequest =
                new ProductVariantCreateRequestDto(
                        "TEST-100",
                        "기본 옵션",
                        new BigDecimal("10000"),
                        ProductVariantStatus.ON_SALE
                );

        ProductCreateRequestDto request =
                new ProductCreateRequestDto(
                        "테스트 상품",
                        ProductStatus.ON_SALE,
                        List.of(variantRequest)
                );

        given(productVariantRepository.existsBySkuCode("TEST-100"))
                .willReturn(false);

        given(productRepository.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        ProductResponseDto response =
                productService.createProduct(request);

        // then
        assertThat(response.name())
                .isEqualTo("테스트 상품");

        assertThat(response.status())
                .isEqualTo(ProductStatus.ON_SALE);

        assertThat(response.variants())
                .hasSize(1);

        assertThat(response.variants().get(0).skuCode())
                .isEqualTo("TEST-100");

        verify(productRepository)
                .save(any(Product.class));
    }

    @Test
    void 이미_존재하는_SKU는_등록할_수_없다() {
        // given
        ProductVariantCreateRequestDto variantRequest =
                new ProductVariantCreateRequestDto(
                        "TEST-002",
                        "중복 옵션",
                        new BigDecimal("10000"),
                        ProductVariantStatus.ON_SALE
                );

        ProductCreateRequestDto request =
                new ProductCreateRequestDto(
                        "중복 SKU 상품",
                        ProductStatus.ON_SALE,
                        List.of(variantRequest)
                );

        given(productVariantRepository.existsBySkuCode("TEST-002"))
                .willReturn(true);

        // when & then
        assertThatThrownBy(
                () -> productService.createProduct(request)
        )
                .isInstanceOf(DuplicateSkuCodeException.class)
                .hasMessageContaining("TEST-002");

        verify(productRepository, never())
                .save(any(Product.class));
    }

    @Test
    void 상품을_단건_조회한다() {
        // given
        Product product =
                new Product(
                        "테스트 상품",
                        ProductStatus.ON_SALE
                );

        ProductVariant variant =
                new ProductVariant(
                        "TEST-200",
                        "기본 옵션",
                        new BigDecimal("15000"),
                        ProductVariantStatus.ON_SALE
                );

        product.addVariant(variant);

        given(productRepository.findWithVariantsById(1L))
                .willReturn(Optional.of(product));

        // when
        ProductResponseDto response =
                productService.getProduct(1L);

        // then
        assertThat(response.name())
                .isEqualTo("테스트 상품");

        assertThat(response.variants())
                .hasSize(1);

        assertThat(response.variants().get(0).skuCode())
                .isEqualTo("TEST-200");
    }

    @Test
    void 존재하지_않는_상품을_조회하면_예외가_발생한다() {
        // given
        given(productRepository.findWithVariantsById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(
                () -> productService.getProduct(999L)
        )
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999");
    }
}