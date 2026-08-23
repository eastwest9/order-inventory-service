package com.eastwest9.orderinventory.inventory.repository;

import com.eastwest9.orderinventory.inventory.domain.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductVariant_Id(Long variantId);

    boolean existsByProductVariant_Id(Long variantId);
}
