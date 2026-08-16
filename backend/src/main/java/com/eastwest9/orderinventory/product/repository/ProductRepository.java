package com.eastwest9.orderinventory.product.repository;

import com.eastwest9.orderinventory.product.domain.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "variants")
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findWithVariantsById(@Param("productId") Long productId);

    @EntityGraph(attributePaths = "variants")
    @Query("""
            SELECT DISTINCT p
            FROM Product p
            ORDER BY p.id DESC
            """)
    List<Product> findAllWithVariants();
}