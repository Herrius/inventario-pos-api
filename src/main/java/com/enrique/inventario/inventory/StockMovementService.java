package com.enrique.inventario.inventory;

import com.enrique.inventario.catalog.Product;
import com.enrique.inventario.catalog.ProductRepository;
import com.enrique.inventario.common.NotFoundException;
import com.enrique.inventario.common.PageResponse;
import com.enrique.inventario.inventory.dto.LowStockResponse;
import com.enrique.inventario.inventory.dto.StockAdjustmentRequest;
import com.enrique.inventario.inventory.dto.StockEntryRequest;
import com.enrique.inventario.inventory.dto.StockMovementResponse;
import com.enrique.inventario.user.User;
import com.enrique.inventario.user.UserNotFoundException;
import com.enrique.inventario.user.UserRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio del corazón de B2.
 *
 * INVARIANTE: toda mutación de {@code Product.stock_actual} ocurre AQUÍ, vía
 * {@link #appendMovement}. Nunca desde {@code ProductService}. Esto garantiza
 * que el log y la proyección queden siempre sincronizados (misma transacción).
 *
 * Concurrencia: el {@code @Version} de {@link Product} hace que dos
 * transacciones simultáneas sobre el mismo producto provoquen
 * {@code OptimisticLockingFailureException} en la perdedora → 409 (handler global).
 */
@Service
public class StockMovementService {

    private final StockMovementRepository repository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public StockMovementService(StockMovementRepository repository,
                                ProductRepository productRepository,
                                UserRepository userRepository) {
        this.repository = repository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public StockMovementResponse registerEntry(StockEntryRequest req, String userEmail) {
        return appendMovement(req.productId(), StockMovementType.ENTRADA,
                req.cantidad(), req.razon(), userEmail, null);
    }

    @Transactional
    public StockMovementResponse registerAdjustment(StockAdjustmentRequest req, String userEmail) {
        StockMovementType tipo = req.direccion() == StockAdjustmentRequest.Direction.POSITIVO
                ? StockMovementType.AJUSTE_POSITIVO
                : StockMovementType.AJUSTE_NEGATIVO;
        return appendMovement(req.productId(), tipo, req.cantidad(), req.razon(), userEmail, null);
    }

    /**
     * Punto único de mutación del stock. Crea un evento append-only y aplica el
     * delta correspondiente a {@code Product.stock_actual} en la MISMA transacción.
     *
     * También es invocado desde {@code SalesService} en M4 (saleId no nulo, tipo SALIDA).
     */
    @Transactional
    public StockMovementResponse appendMovement(Long productId, StockMovementType tipo,
                                                int cantidad, String razon,
                                                String userEmail, Long saleId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: id=" + productId));
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new UserNotFoundException(userEmail));

        // Aplica el delta firmado sobre la proyección. Lanza IllegalStateException
        // si dejaría el stock negativo (capturado abajo y traducido a 409).
        int delta = tipo.sign() * cantidad;
        try {
            product.aplicarDeltaStock(delta);
        } catch (IllegalStateException e) {
            throw new com.enrique.inventario.common.ConflictException(e.getMessage());
        }

        StockMovement movement = new StockMovement(product, tipo, cantidad, razon, user, saleId);
        StockMovement saved = repository.save(movement);
        // El update de product.stock se persiste en el commit (managed entity).
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> search(Long productId,
                                                      OffsetDateTime from,
                                                      OffsetDateTime to,
                                                      Pageable pageable) {
        return PageResponse.from(repository.search(productId, from, to, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public List<LowStockResponse> lowStock() {
        return repository.findProductsBelowMinStock().stream()
                .map(p -> new LowStockResponse(
                        p.getId(), p.getSku(), p.getNombre(),
                        p.getCategory().getNombre(),
                        p.getStockActual(), p.getMinStock(),
                        p.getMinStock() - p.getStockActual()))
                .toList();
    }

    private StockMovementResponse toResponse(StockMovement m) {
        Product p = m.getProduct();
        return new StockMovementResponse(
                m.getId(),
                p.getId(), p.getSku(), p.getNombre(),
                m.getTipo(), m.getCantidad(), m.deltaConSigno(),
                m.getRazon(), m.getSaleId(),
                m.getUser().getEmail(),
                m.getCreatedAt());
    }
}
