package com.enrique.inventario.inventory;

import com.enrique.inventario.catalog.Product;
import com.enrique.inventario.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

/**
 * Evento de movimiento de stock. APPEND-ONLY: no se actualiza, no se borra.
 * Para corregir un error se crea otro movimiento de tipo {@code AJUSTE_*}.
 *
 * El conjunto de movimientos es la verdad del stock; {@link Product#getStockActual()}
 * es solo una proyección/cache que se mantiene en la misma transacción que crea el
 * movimiento (servicio {@link StockMovementService}).
 */
@Entity
@Table(name = "stock_movement")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockMovementType tipo;

    /** Siempre positivo. El signo del efecto lo da {@link #tipo}. */
    @Column(nullable = false)
    private Integer cantidad;

    @Column(columnDefinition = "TEXT")
    private String razon;

    /**
     * Si el movimiento fue causado por una venta, su ID. Hoy es {@code Long} simple;
     * en M4 (cuando exista la tabla {@code sales}) se promueve a relación {@code @ManyToOne}.
     */
    @Column(name = "sale_id")
    private Long saleId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected StockMovement() {
        // Requerido por JPA.
    }

    public StockMovement(Product product, StockMovementType tipo, int cantidad,
                         String razon, User user, Long saleId) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("cantidad debe ser > 0; el signo lo determina el tipo.");
        }
        this.product = product;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.razon = razon;
        this.user = user;
        this.saleId = saleId;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    /** {@code +cantidad} (ENTRADA, AJUSTE_POSITIVO) o {@code -cantidad} (SALIDA, AJUSTE_NEGATIVO). */
    public int deltaConSigno() {
        return tipo.sign() * cantidad;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public StockMovementType getTipo() { return tipo; }
    public Integer getCantidad() { return cantidad; }
    public String getRazon() { return razon; }
    public Long getSaleId() { return saleId; }
    public User getUser() { return user; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
