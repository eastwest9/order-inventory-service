package com.eastwest9.orderinventory.product.service;

import com.eastwest9.orderinventory.product.domain.Product;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.dto.ProductCreateRequestDto;
import com.eastwest9.orderinventory.product.dto.ProductResponseDto;
import com.eastwest9.orderinventory.product.dto.ProductVariantCreateRequestDto;
import com.eastwest9.orderinventory.product.exception.DuplicateSkuCodeException;
import com.eastwest9.orderinventory.product.exception.ProductNotFoundException;
import com.eastwest9.orderinventory.product.repository.ProductRepository;
import com.eastwest9.orderinventory.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto request) {
        validateDuplicateSkuCodes(request);

        Product product = new Product(
                request.name(),
                request.status()
        );

        for (ProductVariantCreateRequestDto variantRequest : request.variants()) {
            validateExistingSkuCode(variantRequest.skuCode());

            ProductVariant variant = new ProductVariant(
                    variantRequest.skuCode(),
                    variantRequest.name(),
                    variantRequest.salePrice(),
                    variantRequest.status()
            );

            product.addVariant(variant);
        }

        Product savedProduct = productRepository.save(product);

        return ProductResponseDto.from(savedProduct);
    }

    public ProductResponseDto getProduct(Long productId) {
        Product product = productRepository.findWithVariantsById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));

        return ProductResponseDto.from(product);
    }

    public List<ProductResponseDto> getProducts() {
        return productRepository.findAllWithVariants()
                .stream()
                .map(ProductResponseDto::from)
                .toList();
    }

    private void validateDuplicateSkuCodes(ProductCreateRequestDto request) {
        Set<String> skuCodes = new HashSet<>();

        for (ProductVariantCreateRequestDto variant : request.variants()) {
            if (!skuCodes.add(variant.skuCode())) {
                throw new DuplicateSkuCodeException(variant.skuCode());
            }
        }
    }

    private void validateExistingSkuCode(String skuCode) {
        if (productVariantRepository.existsBySkuCode(skuCode)) {
            throw new DuplicateSkuCodeException(skuCode);
        }
    }
}