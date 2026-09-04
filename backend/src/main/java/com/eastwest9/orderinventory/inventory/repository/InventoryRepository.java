package com.eastwest9.orderinventory.inventory.repository;

import com.eastwest9.orderinventory.inventory.domain.Inventory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    Optional<Inventory> findByProductVariant_Id(Long variantId);

    @Modifying
    @Query("""
            update Inventory i
            set i.quantity = i.quantity - :quantity
            where i.productVariant.id = :variantId
              and i.quantity >= :quantity
            """)
    int decreaseQuantityIfAvailable(@Param("variantId") Long variantId, @Param("quantity") int quantity);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from Inventory i
            where i.productVariant.id = :variantId
            """)
    Optional<Inventory> findByProductVariantIdForUpdate(@Param("variantId") Long variantId);

    boolean existsByProductVariant_Id(Long variantId);
}
