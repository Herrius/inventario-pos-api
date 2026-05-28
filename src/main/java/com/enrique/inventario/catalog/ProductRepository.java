package com.enrique.inventario.catalog;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    /**
     * Listado paginado con filtros opcionales (categoría, búsqueda por nombre/sku).
     *
     * JOIN FETCH para traer Category en la MISMA query → anti-N+1: con LAZY,
     * cada acceso a {@code product.category.getNombre()} dispararía una query.
     * Con FETCH, Hibernate la trae todo en un solo SELECT.
     *
     * IMPORTANTE: {@code q} se compara contra {@code ''} (string vacío) en lugar de
     * {@code IS NULL}. Cuando un parámetro JPQL llega como Java {@code null},
     * Hibernate hace {@code setObject(null)} sin tipo SQL declarado, y al meterlo
     * en {@code LOWER(...)} Postgres lo deduce como {@code bytea} y falla con
     * "function lower(bytea) does not exist". Pasar siempre un string (vacío para
     * "sin filtro") evita esa inferencia incorrecta.
     *
     * El {@code countQuery} explícito evita el bug "JOIN FETCH no compatible con
     * count": Spring Data NO sabe contar a través de un JOIN FETCH.
     */
    @Query(value = """
            SELECT p FROM Product p
            JOIN FETCH p.category c
            WHERE (:categoryId IS NULL OR c.id = :categoryId)
              AND (:q = ''
                    OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(p.sku)    LIKE LOWER(CONCAT('%', :q, '%')))
            """,
            countQuery = """
            SELECT COUNT(p) FROM Product p
            WHERE (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (:q = ''
                    OR LOWER(p.nombre) LIKE LOWER(CONCAT('%', :q, '%'))
                    OR LOWER(p.sku)    LIKE LOWER(CONCAT('%', :q, '%')))
            """)
    Page<Product> search(@Param("categoryId") Long categoryId,
                         @Param("q") String q,
                         Pageable pageable);

    /** Lookup individual con FETCH de la categoría (evita N+1 al armar el response). */
    @Query("SELECT p FROM Product p JOIN FETCH p.category WHERE p.id = :id")
    Optional<Product> findByIdWithCategory(@Param("id") Long id);
}
