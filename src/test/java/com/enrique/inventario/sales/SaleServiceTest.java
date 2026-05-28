package com.enrique.inventario.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.enrique.inventario.catalog.Category;
import com.enrique.inventario.catalog.Product;
import com.enrique.inventario.catalog.ProductRepository;
import com.enrique.inventario.common.NotFoundException;
import com.enrique.inventario.inventory.StockMovementService;
import com.enrique.inventario.inventory.StockMovementType;
import com.enrique.inventario.sales.dto.SaleItemRequest;
import com.enrique.inventario.sales.dto.SaleRequest;
import com.enrique.inventario.user.Role;
import com.enrique.inventario.user.User;
import com.enrique.inventario.user.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests del corazón transaccional. Toda la persistencia mockeada — el test
 * verifica que la SECUENCIA Y CONDICIONES del flujo sean correctas, no que JPA
 * funcione (eso es trabajo de los tests de integración).
 */
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    @Mock SaleRepository saleRepository;
    @Mock ProductRepository productRepository;
    @Mock UserRepository userRepository;
    @Mock StockMovementService stockMovementService;

    @InjectMocks SaleService service;

    private User cajero;
    private Category bebidas;
    private Product coca;

    @BeforeEach
    void setUp() {
        cajero = new User("cajero@test.com", "hash", Role.CAJERO);
        bebidas = new Category("Bebidas");
        coca = new Product("COCA-500", "Coca Cola 500ml", new BigDecimal("3.50"), bebidas, 10);
    }

    @Test
    void createSale_happyPath_persistsAndAppendsMovements() {
        when(userRepository.findByEmail("cajero@test.com")).thenReturn(Optional.of(cajero));
        when(productRepository.findById(1L)).thenReturn(Optional.of(coca));
        // saleRepository.save devuelve la misma instancia (no le ponemos id porque
        // el flush no se ejecuta en mock; el test no asserts sobre el id).
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        SaleRequest req = new SaleRequest(List.of(new SaleItemRequest(1L, 2)));

        var resp = service.createSale(req, "cajero@test.com");

        assertThat(resp.userEmail()).isEqualTo("cajero@test.com");
        assertThat(resp.status()).isEqualTo(SaleStatus.PAGADA);
        // total = 2 * 3.50 = 7.00 (snapshot del precio actual)
        assertThat(resp.total()).isEqualByComparingTo("7.00");
        assertThat(resp.items()).hasSize(1);
        assertThat(resp.items().get(0).precioUnitario()).isEqualByComparingTo("3.50");

        verify(saleRepository, times(1)).save(any(Sale.class));
        verify(saleRepository, times(1)).flush();
        // saleId va a null porque el mock no asigna el @Id generado por la BD.
        // En producción, después de flush() saleId tiene el valor real. Acá
        // verificamos solo el contrato del invoke con los args que sí controlamos.
        verify(stockMovementService, times(1)).appendMovement(
                eq(1L), eq(StockMovementType.SALIDA), eq(2), anyString(),
                eq("cajero@test.com"), nullable(Long.class));
    }

    @Test
    void createSale_throwsNotFound_whenProductMissing() {
        when(userRepository.findByEmail("cajero@test.com")).thenReturn(Optional.of(cajero));
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        SaleRequest req = new SaleRequest(List.of(new SaleItemRequest(999L, 1)));

        assertThatThrownBy(() -> service.createSale(req, "cajero@test.com"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");

        // Crítico: NUNCA se persiste si un producto falta. Verifica atomicidad
        // a nivel de servicio.
        verify(saleRepository, never()).save(any());
        verify(stockMovementService, never()).appendMovement(
                anyLong(), any(), anyInt(), anyString(), anyString(), nullable(Long.class));
    }

    @Test
    void createSale_takesPriceSnapshotAtSaleTime() {
        // Snapshot defensible en entrevista: si el precio cambia entre que
        // se loadea el producto y se crea el item, lo que cuenta es el precio
        // al momento de construir SaleItem.
        coca.setPrecio(new BigDecimal("99.99"));  // cambiar precio post-carga
        when(userRepository.findByEmail("cajero@test.com")).thenReturn(Optional.of(cajero));
        when(productRepository.findById(1L)).thenReturn(Optional.of(coca));
        when(saleRepository.save(any(Sale.class))).thenAnswer(inv -> inv.getArgument(0));

        var resp = service.createSale(
                new SaleRequest(List.of(new SaleItemRequest(1L, 1))),
                "cajero@test.com");

        assertThat(resp.items().get(0).precioUnitario()).isEqualByComparingTo("99.99");
    }
}
