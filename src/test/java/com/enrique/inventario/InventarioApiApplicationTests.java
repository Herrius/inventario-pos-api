package com.enrique.inventario;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InventarioApiApplicationTests {

    @Test
    void contextLoads() {
        // Verifica que el contexto de Spring arranca completo (datasource, JPA,
        // Flyway) contra un Postgres real levantado por Testcontainers.
    }

}
