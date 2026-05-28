package com.enrique.inventario.sales;

import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRepository extends JpaRepository<Sale, Long> {

    /**
     * Búsqueda paginada con filtros opcionales (rango temporal, cajero).
     * Solo carga la cabecera + user (las líneas se traen aparte vía
     * {@link #findByIdWithItems}).
     */
    @Query(value = """
            SELECT s FROM Sale s
            JOIN FETCH s.user u
            WHERE (cast(:from as timestamp) IS NULL OR s.createdAt >= :from)
              AND (cast(:to   as timestamp) IS NULL OR s.createdAt <= :to)
              AND (:userId IS NULL OR u.id = :userId)
            """,
            countQuery = """
            SELECT COUNT(s) FROM Sale s
            WHERE (cast(:from as timestamp) IS NULL OR s.createdAt >= :from)
              AND (cast(:to   as timestamp) IS NULL OR s.createdAt <= :to)
              AND (:userId IS NULL OR s.user.id = :userId)
            """)
    Page<Sale> search(@Param("from") OffsetDateTime from,
                      @Param("to") OffsetDateTime to,
                      @Param("userId") Long userId,
                      Pageable pageable);

    /**
     * Detalle de una venta con líneas y producto de cada línea cargados.
     * EntityGraph evita el N+1 al armar el response (línea → producto → category).
     *
     * El {@code @Query} explícito es necesario porque Spring Data intenta derivar
     * la query desde el nombre del método y "WithItems" no es una propiedad.
     */
    @EntityGraph(attributePaths = {"user", "items", "items.product", "items.product.category"})
    @Query("SELECT s FROM Sale s WHERE s.id = :id")
    Optional<Sale> findByIdWithItems(@Param("id") Long id);
}
