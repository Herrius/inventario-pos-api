package com.enrique.inventario.inventory;

import com.enrique.inventario.catalog.Product;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    /**
     * Log paginado con filtros opcionales (productId, from, to).
     *
     * JOIN FETCH product + user para evitar N+1 al armar el response (que muestra
     * sku, nombre del producto y email del usuario que generó el movimiento).
     */
    @Query(value = """
            SELECT m FROM StockMovement m
            JOIN FETCH m.product p
            JOIN FETCH m.user u
            WHERE (:productId IS NULL OR p.id = :productId)
              AND (cast(:from as timestamp) IS NULL OR m.createdAt >= :from)
              AND (cast(:to   as timestamp) IS NULL OR m.createdAt <= :to)
            """,
            countQuery = """
            SELECT COUNT(m) FROM StockMovement m
            WHERE (:productId IS NULL OR m.product.id = :productId)
              AND (cast(:from as timestamp) IS NULL OR m.createdAt >= :from)
              AND (cast(:to   as timestamp) IS NULL OR m.createdAt <= :to)
            """)
    Page<StockMovement> search(@Param("productId") Long productId,
                               @Param("from") OffsetDateTime from,
                               @Param("to") OffsetDateTime to,
                               Pageable pageable);

    /** Para LowStockResponse — productos con stockActual < minStock. */
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.stockActual < p.minStock ORDER BY (p.minStock - p.stockActual) DESC")
    java.util.List<Product> findProductsBelowMinStock();
}
