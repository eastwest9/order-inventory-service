package com.eastwest9.orderinventory.inventory.repository;

import com.eastwest9.orderinventory.inventory.domain.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryHistoryRepository
        extends JpaRepository<InventoryHistory, Long> {

    List<InventoryHistory> findAllByProductVariant_IdOrderByIdDesc(Long variantId);
}
