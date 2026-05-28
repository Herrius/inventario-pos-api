package com.enrique.inventario.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    @Column(nullable = false, length = 200)
    private String nombre;

    // BigDecimal con NUMERIC(12,2) — nunca double/float para dinero.
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal precio;

    // LAZY: el listado de productos no siempre necesita la categoría.
    // Para listados que SÍ la muestren, usar JOIN FETCH en la query (anti-N+1).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /**
     * PROYECCIÓN del stock real. La verdad será {@code StockMovement} (M3).
     * Se mantiene aquí como cache para queries rápidas y validación pre-venta;
     * se actualiza DENTRO de la misma transacción que crea el movimiento (M3/M4),
     * y se protege con {@code @Version} para evitar sobreventa concurrente.
     */
    @Column(name = "stock_actual", nullable = false)
    private Integer stockActual;

    @Column(name = "min_stock", nullable = false)
    private Integer minStock;

    /**
     * Optimistic locking. Cuando dos transacciones leen el mismo producto, ambas
     * modifican el stock y commitean, la segunda en llegar recibe
     * {@link org.springframework.orm.ObjectOptimisticLockingFailureException} →
     * 409 ConflictException en el handler. Sin esto: sobreventa.
     */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Product() {
        // Requerido por JPA.
    }

    public Product(String sku, String nombre, BigDecimal precio, Category category, Integer minStock) {
        this.sku = sku;
        this.nombre = nombre;
        this.precio = precio;
        this.category = category;
        this.stockActual = 0;       // se inicia en 0; entra stock vía StockMovement en M3
        this.minStock = minStock != null ? minStock : 0;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (stockActual == null) stockActual = 0;
        if (minStock == null) minStock = 0;
        if (version == null) version = 0L;
    }

    public Long getId() { return id; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public BigDecimal getPrecio() { return precio; }
    public Category getCategory() { return category; }
    public Integer getStockActual() { return stockActual; }
    public Integer getMinStock() { return minStock; }
    public Long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setSku(String sku) { this.sku = sku; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setPrecio(BigDecimal precio) { this.precio = precio; }
    public void setCategory(Category category) { this.category = category; }
    public void setMinStock(Integer minStock) { this.minStock = minStock; }

    /**
     * Aplica un delta al stock actual (positivo = entrada/ajuste suma, negativo = salida/ajuste resta).
     * Se invoca desde el service de inventario (M3) o el de ventas (M4) DENTRO de
     * una transacción. JPA registra el cambio y, al hacer flush, valida la
     * {@code @Version} — si otra transacción tocó el producto, lanza
     * {@code OptimisticLockingFailureException}.
     *
     * Rechaza el delta si dejaría {@code stock_actual} negativo (constraint en BD
     * también lo rechazaría, pero acá lo capturamos antes para devolver 409 limpio).
     */
    public void aplicarDeltaStock(int delta) {
        int nuevo = this.stockActual + delta;
        if (nuevo < 0) {
            throw new IllegalStateException(
                "Stock insuficiente para sku=" + sku + " (actual=" + stockActual + ", delta=" + delta + ")");
        }
        this.stockActual = nuevo;
    }
}
