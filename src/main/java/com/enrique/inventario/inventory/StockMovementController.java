package com.enrique.inventario.inventory;

import com.enrique.inventario.common.PageResponse;
import com.enrique.inventario.inventory.dto.LowStockResponse;
import com.enrique.inventario.inventory.dto.StockAdjustmentRequest;
import com.enrique.inventario.inventory.dto.StockEntryRequest;
import com.enrique.inventario.inventory.dto.StockMovementResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gestión de movimientos de stock (entradas, ajustes y consulta del log).
 *  - Entradas/ajustes: ADMIN. El cajero no toca stock; solo lo descuenta vía ventas (M4).
 *  - Log y low-stock: cualquier autenticado (cajero necesita ver bajo mínimo).
 */
@RestController
@RequestMapping("/v1/inventory")
public class StockMovementController {

    private final StockMovementService service;

    public StockMovementController(StockMovementService service) {
        this.service = service;
    }

    @PostMapping("/entries")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StockMovementResponse registerEntry(@Valid @RequestBody StockEntryRequest request,
                                               @AuthenticationPrincipal Jwt jwt) {
        return service.registerEntry(request, jwt.getSubject());
    }

    @PostMapping("/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public StockMovementResponse registerAdjustment(@Valid @RequestBody StockAdjustmentRequest request,
                                                    @AuthenticationPrincipal Jwt jwt) {
        return service.registerAdjustment(request, jwt.getSubject());
    }

    @GetMapping("/movements")
    public PageResponse<StockMovementResponse> movements(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @PageableDefault(size = 50, sort = "createdAt") Pageable pageable) {
        return service.search(productId, from, to, pageable);
    }

    @GetMapping("/low-stock")
    public List<LowStockResponse> lowStock() {
        return service.lowStock();
    }
}
