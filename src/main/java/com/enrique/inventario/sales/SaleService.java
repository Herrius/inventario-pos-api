package com.enrique.inventario.sales;

import com.enrique.inventario.catalog.Product;
import com.enrique.inventario.catalog.ProductRepository;
import com.enrique.inventario.common.NotFoundException;
import com.enrique.inventario.common.PageResponse;
import com.enrique.inventario.inventory.StockMovementService;
import com.enrique.inventario.inventory.StockMovementType;
import com.enrique.inventario.sales.dto.SaleItemRequest;
import com.enrique.inventario.sales.dto.SaleItemResponse;
import com.enrique.inventario.sales.dto.SaleRequest;
import com.enrique.inventario.sales.dto.SaleResponse;
import com.enrique.inventario.user.User;
import com.enrique.inventario.user.UserNotFoundException;
import com.enrique.inventario.user.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Diferenciador #2 del proyecto: la venta POS como TRANSACCIÓN ATÓMICA multi-tabla.
 *
 * Una venta toca 3 tablas en una sola unidad atómica:
 *   1. {@code sales}            — cabecera (PAGADA en MVP).
 *   2. {@code sale_items}       — N líneas con snapshot de precio.
 *   3. {@code stock_movement}   — N movimientos SALIDA + decremento de stock
 *                                 con {@code @Version} (en Product).
 *
 * Si CUALQUIER paso falla (stock insuficiente, producto inexistente, conflicto
 * de versión por otro cajero), TODO se revierte. No queda venta sin movimientos
 * ni stock descontado sin venta.
 *
 * Concurrencia: cuando dos cajeros venden simultáneamente la última unidad de
 * un producto, ambos cargan {@code Product} con la misma {@code @Version}. Al
 * commit, el primero gana; el segundo recibe {@code OptimisticLockingFailureException}
 * → 409 OPTIMISTIC_LOCK_FAILURE (handler global). El cajero del POS reintenta;
 * la segunda vez ve stock 0 y obtiene 409 CONFLICT por stock insuficiente.
 *
 * El diseño del log (M3) garantiza que cada SALIDA queda auditada con saleId.
 */
@Service
public class SaleService {

    private final SaleRepository saleRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StockMovementService stockMovementService;

    public SaleService(SaleRepository saleRepository,
                       ProductRepository productRepository,
                       UserRepository userRepository,
                       StockMovementService stockMovementService) {
        this.saleRepository = saleRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.stockMovementService = stockMovementService;
    }

    @Transactional
    public SaleResponse createSale(SaleRequest request, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));

        Sale sale = new Sale(user);

        // Construir las líneas con SNAPSHOT del precio actual. Si un product
        // no existe acá ya se aborta (404) antes de cualquier escritura.
        for (SaleItemRequest line : request.items()) {
            Product product = productRepository.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Producto no encontrado: id=" + line.productId()));
            SaleItem item = new SaleItem(product, line.cantidad(), product.getPrecio());
            sale.addItem(item);
        }
        sale.recalcularTotal();

        // Persistir la venta (cascade ALL → también líneas). Después de save(),
        // sale.id está poblado y se puede pasar al stock movement como saleId.
        Sale saved = saleRepository.save(sale);
        saleRepository.flush();  // fuerza el INSERT y obtiene los IDs (importante para el log con saleId real).

        // Por cada línea, crear el StockMovement de SALIDA (descuenta stock + crea evento).
        // appendMovement participa en esta misma transacción (REQUIRED por defecto).
        // Si stock insuficiente o @Version conflict, lanza excepción y todo se revierte.
        for (SaleItem item : saved.getItems()) {
            stockMovementService.appendMovement(
                    item.getProduct().getId(),
                    StockMovementType.SALIDA,
                    item.getCantidad(),
                    "Venta #" + saved.getId(),
                    userEmail,
                    saved.getId());
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public SaleResponse findById(Long id) {
        Sale sale = saleRepository.findByIdWithItems(id)
                .orElseThrow(() -> new NotFoundException("Venta no encontrada: id=" + id));
        return toResponse(sale);
    }

    @Transactional(readOnly = true)
    public PageResponse<SaleResponse> search(OffsetDateTime from,
                                             OffsetDateTime to,
                                             Long userId,
                                             Pageable pageable) {
        // Para el listado, devolvemos cabeceras "ligeras" sin las líneas
        // (evita N+1 sin gracia). Si el cliente necesita las líneas, llama GET /{id}.
        return PageResponse.from(
                saleRepository.search(from, to, userId, pageable).map(this::toResponseHeaderOnly));
    }

    private SaleResponse toResponse(Sale s) {
        var items = s.getItems().stream()
                .map(i -> new SaleItemResponse(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getSku(),
                        i.getProduct().getNombre(),
                        i.getCantidad(),
                        i.getPrecioUnitario(),
                        i.getSubtotal()))
                .toList();
        return new SaleResponse(
                s.getId(), s.getUser().getEmail(), s.getTotal(),
                s.getStatus(), s.getCreatedAt(), items);
    }

    /** Para el listado: cabecera sin líneas (evita N+1). */
    private SaleResponse toResponseHeaderOnly(Sale s) {
        return new SaleResponse(
                s.getId(), s.getUser().getEmail(), s.getTotal(),
                s.getStatus(), s.getCreatedAt(), java.util.List.of());
    }
}
