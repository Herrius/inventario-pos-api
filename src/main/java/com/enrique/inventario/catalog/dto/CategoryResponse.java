package com.enrique.inventario.catalog.dto;

import java.time.OffsetDateTime;

public record CategoryResponse(
        Long id,
        String nombre,
        OffsetDateTime createdAt
) {}
