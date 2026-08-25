package com.eastwest9.orderinventory.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record OrderCreateRequestDto(
        @NotNull(message = "회원 ID는 필수입니다.")
        Long memberId,

        @NotEmpty(message = "주문 항목은 하나 이상이어야 합니다.")
        List<@Valid OrderItemCreateRequestDto> items
) {
}
