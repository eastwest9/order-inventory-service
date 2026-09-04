package com.eastwest9.orderinventory.product.controller;

import com.eastwest9.orderinventory.product.dto.ProductCreateRequestDto;
import com.eastwest9.orderinventory.product.dto.ProductResponseDto;
import com.eastwest9.orderinventory.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductResponseDto> createProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        ProductResponseDto response = productService.createProduct(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponseDto> getProduct(@PathVariable Long productId) {
        ProductResponseDto response = productService.getProduct(productId);

        return ResponseEntity.ok(response);
    }
    @GetMapping
    public ResponseEntity<List<ProductResponseDto>> getProducts() {
        List<ProductResponseDto> response = productService.getProducts();

        return ResponseEntity.ok(response);
    }
}
