package com.eastwest9.orderinventory.product.dto;

import com.eastwest9.orderinventory.product.domain.ProductStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record ProductCreateRequestDto(

        @NotBlank(message = "상품명은 필수입니다.")
        @Size(max = 200, message = "상품명은 200자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "상품 상태는 필수입니다.")
        ProductStatus status,

        @NotEmpty(message = "상품은 하나 이상의 SKU를 가져야 합니다.")
        List<@Valid ProductVariantCreateRequestDto> variants
) {
}