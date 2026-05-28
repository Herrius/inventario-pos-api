package com.enrique.inventario.sales;

import com.enrique.inventario.common.PageResponse;
import com.enrique.inventario.sales.dto.SaleRequest;
import com.enrique.inventario.sales.dto.SaleResponse;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Registro y consulta de ventas POS.
 *  - POST: cualquier autenticado (CAJERO o ADMIN). El email del JWT es el cajero.
 *  - GET: cualquier autenticado. Sin filtro de owner — en un POS la visibilidad
 *    es del negocio, no por cajero.
 */
@RestController
@RequestMapping("/v1/sales")
public class SaleController {

    private final SaleService service;

    public SaleController(SaleService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SaleResponse create(@Valid @RequestBody SaleRequest request,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.createSale(request, jwt.getSubject());
    }

    @GetMapping("/{id}")
    public SaleResponse get(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping
    public PageResponse<SaleResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            @RequestParam(required = false) Long userId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return service.search(from, to, userId, pageable);
    }
}
