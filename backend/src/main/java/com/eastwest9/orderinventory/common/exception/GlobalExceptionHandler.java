package com.eastwest9.orderinventory.common.exception;

import com.eastwest9.orderinventory.product.exception.DuplicateSkuCodeException;
import com.eastwest9.orderinventory.product.exception.ProductNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductNotFound(
            ProductNotFoundException exception
    ) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "PRODUCT_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(DuplicateSkuCodeException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateSkuCode(
            DuplicateSkuCodeException exception
    ) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "DUPLICATE_SKU_CODE",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("요청 값이 올바르지 않습니다.");

        ErrorResponseDto response = ErrorResponseDto.of(
                "INVALID_REQUEST",
                message
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }
}