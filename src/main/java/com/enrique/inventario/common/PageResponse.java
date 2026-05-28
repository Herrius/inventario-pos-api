package com.enrique.inventario.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Contrato de respuesta paginada estable. Evitamos serializar Spring `Page<T>`
 * directamente (su formato JSON no es estable y Spring avisa de ello).
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> from(Page<T> p) {
        return new PageResponse<>(p.getContent(), p.getNumber(), p.getSize(),
                p.getTotalElements(), p.getTotalPages());
    }
}
