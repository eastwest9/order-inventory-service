package com.eastwest9.orderinventory.common.exception;

import com.eastwest9.orderinventory.inventory.exception.DuplicateInventoryException;
import com.eastwest9.orderinventory.inventory.exception.InsufficientInventoryException;
import com.eastwest9.orderinventory.inventory.exception.InvalidInventoryQuantityException;
import com.eastwest9.orderinventory.inventory.exception.InventoryNotFoundException;
import com.eastwest9.orderinventory.member.exception.MemberNotFoundException;
import com.eastwest9.orderinventory.order.exception.InvalidOrderException;
import com.eastwest9.orderinventory.order.exception.OrderAlreadyCanceledException;
import com.eastwest9.orderinventory.order.exception.OrderNotFoundException;
import com.eastwest9.orderinventory.product.exception.DuplicateSkuCodeException;
import com.eastwest9.orderinventory.product.exception.ProductNotFoundException;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotFoundException;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotOrderableException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MemberNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleMemberNotFound(MemberNotFoundException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "MEMBER_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleOrderNotFound(OrderNotFoundException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "ORDER_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(ProductVariantNotOrderableException.class)
    public ResponseEntity<ErrorResponseDto> handleProductVariantNotOrderable(ProductVariantNotOrderableException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "PRODUCT_VARIANT_NOT_ORDERABLE",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(OrderAlreadyCanceledException.class)
    public ResponseEntity<ErrorResponseDto> handleOrderAlreadyCanceled(OrderAlreadyCanceledException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "ORDER_ALREADY_CANCELED",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidOrderException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidOrder(InvalidOrderException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "INVALID_ORDER",
                exception.getMessage()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductNotFound(ProductNotFoundException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "PRODUCT_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(DuplicateSkuCodeException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateSkuCode(DuplicateSkuCodeException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "DUPLICATE_SKU_CODE",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(ProductVariantNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleProductVariantNotFound(ProductVariantNotFoundException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "PRODUCT_VARIANT_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(InventoryNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> handleInventoryNotFound(InventoryNotFoundException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "INVENTORY_NOT_FOUND",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    @ExceptionHandler(InsufficientInventoryException.class)
    public ResponseEntity<ErrorResponseDto> handleInsufficientInventory(InsufficientInventoryException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "INSUFFICIENT_INVENTORY",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponseDto> handleOptimisticLockingFailure(OptimisticLockingFailureException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "INVENTORY_CONFLICT",
                "동시 재고 변경으로 주문 처리에 실패했습니다."
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(DuplicateInventoryException.class)
    public ResponseEntity<ErrorResponseDto> handleDuplicateInventory(DuplicateInventoryException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "DUPLICATE_INVENTORY",
                exception.getMessage()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    @ExceptionHandler(InvalidInventoryQuantityException.class)
    public ResponseEntity<ErrorResponseDto> handleInvalidInventoryQuantity(InvalidInventoryQuantityException exception) {
        ErrorResponseDto response = ErrorResponseDto.of(
                "INVALID_INVENTORY_QUANTITY",
                exception.getMessage()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> handleValidation(MethodArgumentNotValidException exception) {
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
