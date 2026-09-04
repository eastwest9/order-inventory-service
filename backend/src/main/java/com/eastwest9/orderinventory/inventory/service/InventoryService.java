package com.eastwest9.orderinventory.inventory.service;

import com.eastwest9.orderinventory.inventory.domain.Inventory;
import com.eastwest9.orderinventory.inventory.domain.InventoryHistory;
import com.eastwest9.orderinventory.inventory.dto.InventoryAdjustRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryCreateRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryHistoryResponseDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryReceiveRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryResponseDto;
import com.eastwest9.orderinventory.inventory.exception.DuplicateInventoryException;
import com.eastwest9.orderinventory.inventory.exception.InventoryNotFoundException;
import com.eastwest9.orderinventory.inventory.repository.InventoryHistoryRepository;
import com.eastwest9.orderinventory.inventory.repository.InventoryRepository;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotFoundException;
import com.eastwest9.orderinventory.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;
    private final ProductVariantRepository productVariantRepository;

    @Transactional
    public InventoryResponseDto createInventory(InventoryCreateRequestDto request) {
        ProductVariant productVariant = productVariantRepository
                .findById(request.variantId())
                .orElseThrow(() -> new ProductVariantNotFoundException(
                        request.variantId()
                ));

        if (inventoryRepository.existsByProductVariant_Id(request.variantId())) {
            throw new DuplicateInventoryException(request.variantId());
        }

        Inventory inventory = new Inventory(
                productVariant,
                request.initialQuantity()
        );
        Inventory savedInventory = inventoryRepository.save(inventory);

        InventoryHistory history = InventoryHistory.initial(
                productVariant,
                request.initialQuantity()
        );
        inventoryHistoryRepository.save(history);

        return InventoryResponseDto.from(savedInventory);
    }

    public InventoryResponseDto getInventory(Long variantId) {
        return InventoryResponseDto.from(findInventory(variantId));
    }

    @Transactional
    public InventoryResponseDto receiveInventory(Long variantId, InventoryReceiveRequestDto request) {
        Inventory inventory = findInventory(variantId);
        int beforeQuantity = inventory.getQuantity();

        inventory.receive(request.quantity());

        InventoryHistory history = InventoryHistory.receipt(
                inventory.getProductVariant(),
                beforeQuantity,
                inventory.getQuantity()
        );
        inventoryHistoryRepository.save(history);
        inventoryRepository.flush();

        return InventoryResponseDto.from(inventory);
    }

    @Transactional
    public InventoryResponseDto adjustInventory(Long variantId, InventoryAdjustRequestDto request) {
        Inventory inventory = findInventory(variantId);
        int beforeQuantity = inventory.getQuantity();

        inventory.adjust(request.quantity());

        InventoryHistory history = InventoryHistory.adjustment(
                inventory.getProductVariant(),
                beforeQuantity,
                inventory.getQuantity()
        );
        inventoryHistoryRepository.save(history);
        inventoryRepository.flush();

        return InventoryResponseDto.from(inventory);
    }

    public List<InventoryHistoryResponseDto> getInventoryHistories(Long variantId) {
        if (!inventoryRepository.existsByProductVariant_Id(variantId)) {
            throw new InventoryNotFoundException(variantId);
        }

        return inventoryHistoryRepository
                .findAllByProductVariant_IdOrderByIdDesc(variantId)
                .stream()
                .map(InventoryHistoryResponseDto::from)
                .toList();
    }

    private Inventory findInventory(Long variantId) {
        return inventoryRepository.findByProductVariant_Id(variantId)
                .orElseThrow(() -> new InventoryNotFoundException(variantId));
    }
}
