package com.eastwest9.orderinventory.common.exception;

import java.time.LocalDateTime;

public record ErrorResponseDto(
        String code,
        String message,
        LocalDateTime timestamp
) {

    public static ErrorResponseDto of(
            String code,
            String message
    ) {
        return new ErrorResponseDto(
                code,
                message,
                LocalDateTime.now()
        );
    }
}