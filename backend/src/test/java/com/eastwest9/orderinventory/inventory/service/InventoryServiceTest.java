package com.eastwest9.orderinventory.inventory.service;

import com.eastwest9.orderinventory.inventory.domain.Inventory;
import com.eastwest9.orderinventory.inventory.domain.InventoryChangeType;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void 초기_재고를_생성하고_INITIAL_이력을_저장한다() {
        // given
        ProductVariant productVariant = createProductVariant();
        given(productVariant.getId()).willReturn(1L);
        given(productVariant.getSkuCode()).willReturn("TEST-001");
        InventoryCreateRequestDto request = new InventoryCreateRequestDto(1L, 100);

        given(productVariantRepository.findById(1L))
                .willReturn(Optional.of(productVariant));
        given(inventoryRepository.existsByProductVariant_Id(1L))
                .willReturn(false);
        given(inventoryRepository.save(any(Inventory.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        InventoryResponseDto response = inventoryService.createInventory(request);

        // then
        assertThat(response.variantId()).isEqualTo(1L);
        assertThat(response.skuCode()).isEqualTo("TEST-001");
        assertThat(response.quantity()).isEqualTo(100);

        verify(inventoryRepository).save(any(Inventory.class));

        ArgumentCaptor<InventoryHistory> historyCaptor =
                ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());

        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.INITIAL);
        assertThat(history.getBeforeQuantity()).isZero();
        assertThat(history.getChangeQuantity()).isEqualTo(100);
        assertThat(history.getAfterQuantity()).isEqualTo(100);
    }

    @Test
    void 존재하지_않는_SKU로_초기_재고를_생성할_수_없다() {
        // given
        InventoryCreateRequestDto request = new InventoryCreateRequestDto(999L, 100);
        given(productVariantRepository.findById(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(ProductVariantNotFoundException.class)
                .hasMessageContaining("999");

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(inventoryHistoryRepository, never()).save(any(InventoryHistory.class));
    }

    @Test
    void 이미_재고가_있는_SKU를_다시_초기화할_수_없다() {
        // given
        ProductVariant productVariant = createProductVariant();
        InventoryCreateRequestDto request = new InventoryCreateRequestDto(1L, 100);

        given(productVariantRepository.findById(1L))
                .willReturn(Optional.of(productVariant));
        given(inventoryRepository.existsByProductVariant_Id(1L))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> inventoryService.createInventory(request))
                .isInstanceOf(DuplicateInventoryException.class)
                .hasMessageContaining("1");

        verify(inventoryRepository, never()).save(any(Inventory.class));
        verify(inventoryHistoryRepository, never()).save(any(InventoryHistory.class));
    }

    @Test
    void 현재_재고를_조회한다() {
        // given
        ProductVariant productVariant = createProductVariant();
        given(productVariant.getId()).willReturn(1L);
        given(productVariant.getSkuCode()).willReturn("TEST-001");
        Inventory inventory = new Inventory(productVariant, 100);
        given(inventoryRepository.findByProductVariant_Id(1L))
                .willReturn(Optional.of(inventory));

        // when
        InventoryResponseDto response = inventoryService.getInventory(1L);

        // then
        assertThat(response.variantId()).isEqualTo(1L);
        assertThat(response.skuCode()).isEqualTo("TEST-001");
        assertThat(response.quantity()).isEqualTo(100);
    }

    @Test
    void 존재하지_않는_재고를_조회하면_예외가_발생한다() {
        // given
        given(inventoryRepository.findByProductVariant_Id(999L))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> inventoryService.getInventory(999L))
                .isInstanceOf(InventoryNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    void 재고를_입고하고_RECEIPT_이력을_저장한다() {
        // given
        ProductVariant productVariant = createProductVariant();
        given(productVariant.getId()).willReturn(1L);
        given(productVariant.getSkuCode()).willReturn("TEST-001");
        Inventory inventory = new Inventory(productVariant, 100);
        given(inventoryRepository.findByProductVariant_Id(1L))
                .willReturn(Optional.of(inventory));

        // when
        InventoryResponseDto response = inventoryService.receiveInventory(
                1L,
                new InventoryReceiveRequestDto(30)
        );

        // then
        assertThat(response.quantity()).isEqualTo(130);

        ArgumentCaptor<InventoryHistory> historyCaptor =
                ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());

        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.RECEIPT);
        assertThat(history.getBeforeQuantity()).isEqualTo(100);
        assertThat(history.getChangeQuantity()).isEqualTo(30);
        assertThat(history.getAfterQuantity()).isEqualTo(130);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void 재고를_조정하고_ADJUSTMENT_이력을_저장한다() {
        // given
        ProductVariant productVariant = createProductVariant();
        given(productVariant.getId()).willReturn(1L);
        given(productVariant.getSkuCode()).willReturn("TEST-001");
        Inventory inventory = new Inventory(productVariant, 130);
        given(inventoryRepository.findByProductVariant_Id(1L))
                .willReturn(Optional.of(inventory));

        // when
        InventoryResponseDto response = inventoryService.adjustInventory(
                1L,
                new InventoryAdjustRequestDto(120)
        );

        // then
        assertThat(response.quantity()).isEqualTo(120);

        ArgumentCaptor<InventoryHistory> historyCaptor =
                ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());

        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.ADJUSTMENT);
        assertThat(history.getBeforeQuantity()).isEqualTo(130);
        assertThat(history.getChangeQuantity()).isEqualTo(-10);
        assertThat(history.getAfterQuantity()).isEqualTo(120);
        verify(inventoryRepository, never()).save(any(Inventory.class));
    }

    @Test
    void 재고_이력을_Repository_조회_순서대로_반환한다() {
        // given
        ProductVariant productVariant = createProductVariant();
        given(productVariant.getId()).willReturn(1L);
        InventoryHistory latest = InventoryHistory.receipt(
                productVariant,
                100,
                130
        );
        InventoryHistory oldest = InventoryHistory.initial(productVariant, 100);

        given(inventoryRepository.existsByProductVariant_Id(1L))
                .willReturn(true);
        given(inventoryHistoryRepository
                .findAllByProductVariant_IdOrderByIdDesc(1L))
                .willReturn(List.of(latest, oldest));

        // when
        List<InventoryHistoryResponseDto> response =
                inventoryService.getInventoryHistories(1L);

        // then
        assertThat(response)
                .extracting(InventoryHistoryResponseDto::changeType)
                .containsExactly(
                        InventoryChangeType.RECEIPT,
                        InventoryChangeType.INITIAL
                );
        assertThat(response.get(0).changeQuantity()).isEqualTo(30);
        assertThat(response.get(1).changeQuantity()).isEqualTo(100);
    }

    private ProductVariant createProductVariant() {
        return mock(ProductVariant.class);
    }
}
