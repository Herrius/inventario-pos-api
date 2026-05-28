package com.enrique.inventario.sales;

import com.enrique.inventario.catalog.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/**
 * Línea de una venta. precio_unitario es SNAPSHOT del precio del producto al
 * momento de la venta — la venta pasada no cambia si mañana se ajusta el precio.
 * subtotal es denormalizado (= cantidad * precio_unitario) para que los reportes
 * sumen sin recalcular y como auditoría del importe cobrado real.
 */
@Entity
@Table(name = "sale_items")
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_id", nullable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioUnitario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    protected SaleItem() {
        // Requerido por JPA.
    }

    public SaleItem(Product product, int cantidad, BigDecimal precioUnitarioSnapshot) {
        if (cantidad <= 0) throw new IllegalArgumentException("cantidad debe ser > 0");
        this.product = product;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitarioSnapshot;
        this.subtotal = precioUnitarioSnapshot.multiply(BigDecimal.valueOf(cantidad));
    }

    public Long getId() { return id; }
    public Sale getSale() { return sale; }
    public Product getProduct() { return product; }
    public Integer getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public BigDecimal getSubtotal() { return subtotal; }

    void setSale(Sale sale) { this.sale = sale; }
}
