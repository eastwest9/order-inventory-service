package com.eastwest9.orderinventory.product.dto;

import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ProductVariantCreateRequestDto(

        @NotBlank(message = "SKU 코드는 필수입니다.")
        @Size(max = 100, message = "SKU 코드는 100자를 초과할 수 없습니다.")
        String skuCode,

        @NotBlank(message = "SKU명은 필수입니다.")
        @Size(max = 200, message = "SKU명은 200자를 초과할 수 없습니다.")
        String name,

        @NotNull(message = "판매가격은 필수입니다.")
        @DecimalMin(value = "0.00", message = "판매가격은 0 이상이어야 합니다.")
        @Digits(integer = 17, fraction = 2, message = "판매가격 형식이 올바르지 않습니다.")
        BigDecimal salePrice,

        @NotNull(message = "SKU 상태는 필수입니다.")
        ProductVariantStatus status
) {
}