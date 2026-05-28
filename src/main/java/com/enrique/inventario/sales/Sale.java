package com.enrique.inventario.sales;

import com.enrique.inventario.user.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sales")
public class Sale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SaleStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    // Cascade ALL: al persistir Sale se persisten los items en la misma operación.
    // orphanRemoval=true por si alguna vez se descarta una línea (no en B2 MVP).
    @OneToMany(mappedBy = "sale", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("id ASC")
    private List<SaleItem> items = new ArrayList<>();

    protected Sale() {
        // Requerido por JPA.
    }

    public Sale(User user) {
        this.user = user;
        this.status = SaleStatus.PAGADA;  // POS contado: nace pagada
        this.total = BigDecimal.ZERO;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
    }

    /** Agrega una línea y la enlaza al Sale (bidireccional). */
    public void addItem(SaleItem item) {
        items.add(item);
        item.setSale(this);
    }

    /** Suma {@code subtotal} de cada línea (usado al cerrar la venta). */
    public void recalcularTotal() {
        this.total = items.stream()
                .map(SaleItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public BigDecimal getTotal() { return total; }
    public SaleStatus getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public List<SaleItem> getItems() { return items; }

    public void setStatus(SaleStatus status) { this.status = status; }
}
