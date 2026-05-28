package com.enrique.inventario.reports;

import com.enrique.inventario.reports.dto.SalesByDayEntry;
import com.enrique.inventario.reports.dto.SalesDailySummaryResponse;
import com.enrique.inventario.reports.dto.TopProductEntry;
import com.enrique.inventario.sales.Sale;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Queries de reportes. Se exponen como nativeQuery solo cuando hace falta una
 * función de Postgres específica (DATE_TRUNC); el resto va en JPQL para
 * mantener la portabilidad y dejar que el optimizador haga su trabajo.
 *
 * El repository hereda de JpaRepository<Sale, Long> únicamente para reusar la
 * infraestructura de Spring Data — no se usa la entidad directamente acá; todas
 * las consultas devuelven proyecciones (records) directamente.
 */
public interface ReportRepository extends JpaRepository<Sale, Long> {

    /**
     * Resumen agregado para un rango [from, to). Solo cuenta ventas PAGADAS.
     * COALESCE para no devolver NULL cuando no hay ventas en el rango.
     *
     * Plan ideal: Index Scan en idx_sales_status_time (status='PAGADA' AND created_at IN rango)
     * → Aggregate. Verificable con EXPLAIN ANALYZE.
     *
     * Devuelve una proyección plana en lugar de DTO constructor (la JPA
     * constructor expression de Hibernate 7 falla en match con Long/long del record).
     */
    @Query(value = """
            SELECT COALESCE(COUNT(*), 0)                              AS total_ventas,
                   COALESCE(SUM(total), 0)::numeric(14,2)             AS revenue,
                   COALESCE(AVG(total), 0)::numeric(14,2)             AS ticket_promedio
            FROM sales
            WHERE status = 'PAGADA'
              AND created_at >= :from
              AND created_at <  :to
            """,
            nativeQuery = true)
    DailySummaryProjection summaryForRange(@Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to);

    interface DailySummaryProjection {
        long getTotalVentas();
        java.math.BigDecimal getRevenue();
        java.math.BigDecimal getTicketPromedio();
    }

    /**
     * Ventas agregadas POR DÍA para un rango. Usa DATE_TRUNC de Postgres
     * → nativeQuery. Devuelve filas (dia, total_ventas, revenue) ordenadas
     * por día ascendente.
     *
     * Cast explícito a UTC en el query: lo que entra es OffsetDateTime; lo que
     * sale es la fecha UTC. Para tener fecha local del negocio (M7), aplicar
     * AT TIME ZONE 'America/Lima' a created_at.
     */
    @Query(value = """
            SELECT date_trunc('day', created_at)::date    AS dia,
                   COUNT(*)                                AS total_ventas,
                   COALESCE(SUM(total), 0)::numeric(14,2)  AS revenue
            FROM sales
            WHERE status = 'PAGADA'
              AND created_at >= :from
              AND created_at <  :to
            GROUP BY dia
            ORDER BY dia
            """,
            nativeQuery = true)
    List<SalesByDayProjection> salesByDay(@Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to);

    /**
     * Top productos por unidades vendidas en un rango. JOIN sale_items → sales
     * filtrado por status + rango temporal; GROUP BY producto; ORDER BY unidades DESC LIMIT.
     */
    @Query(value = """
            SELECT p.id                              AS product_id,
                   p.sku                             AS sku,
                   p.nombre                          AS nombre,
                   SUM(si.cantidad)                  AS unidades_vendidas,
                   COALESCE(SUM(si.subtotal), 0)::numeric(14,2)
                                                     AS revenue
            FROM sale_items si
            JOIN sales s    ON s.id = si.sale_id
            JOIN products p ON p.id = si.product_id
            WHERE s.status = 'PAGADA'
              AND s.created_at >= :from
              AND s.created_at <  :to
            GROUP BY p.id, p.sku, p.nombre
            ORDER BY unidades_vendidas DESC
            LIMIT :limit
            """,
            nativeQuery = true)
    List<TopProductProjection> topProducts(@Param("from") OffsetDateTime from,
                                           @Param("to") OffsetDateTime to,
                                           @Param("limit") int limit);

    /** Proyecciones de interface para los queries nativas. Spring Data las cumple
     *  por nombre de método (getter) a alias del SELECT. */
    interface SalesByDayProjection {
        java.time.LocalDate getDia();
        long getTotalVentas();
        java.math.BigDecimal getRevenue();
    }

    interface TopProductProjection {
        Long getProductId();
        String getSku();
        String getNombre();
        long getUnidadesVendidas();
        java.math.BigDecimal getRevenue();
    }
}
