package com.eastwest9.orderinventory.inventory.controller;

import com.eastwest9.orderinventory.inventory.dto.InventoryAdjustRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryCreateRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryHistoryResponseDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryReceiveRequestDto;
import com.eastwest9.orderinventory.inventory.dto.InventoryResponseDto;
import com.eastwest9.orderinventory.inventory.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @PostMapping
    public ResponseEntity<InventoryResponseDto> createInventory(
            @Valid @RequestBody InventoryCreateRequestDto request
    ) {
        InventoryResponseDto response = inventoryService.createInventory(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<InventoryResponseDto> getInventory(
            @PathVariable Long variantId
    ) {
        InventoryResponseDto response = inventoryService.getInventory(variantId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{variantId}/receipts")
    public ResponseEntity<InventoryResponseDto> receiveInventory(
            @PathVariable Long variantId,
            @Valid @RequestBody InventoryReceiveRequestDto request
    ) {
        InventoryResponseDto response = inventoryService.receiveInventory(
                variantId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{variantId}/quantity")
    public ResponseEntity<InventoryResponseDto> adjustInventory(
            @PathVariable Long variantId,
            @Valid @RequestBody InventoryAdjustRequestDto request
    ) {
        InventoryResponseDto response = inventoryService.adjustInventory(
                variantId,
                request
        );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{variantId}/histories")
    public ResponseEntity<List<InventoryHistoryResponseDto>>
    getInventoryHistories(@PathVariable Long variantId) {
        List<InventoryHistoryResponseDto> response =
                inventoryService.getInventoryHistories(variantId);

        return ResponseEntity.ok(response);
    }
}
