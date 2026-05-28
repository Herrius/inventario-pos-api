package com.enrique.inventario;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;

@TestConfiguration(proxyBeanMethods = false)
class TestcontainersConfiguration {

    // @ServiceConnection cablea el datasource de Spring a este Postgres efímero:
    // el test corre contra un Postgres REAL (como producción), no contra un H2
    // que mentiría sobre el comportamiento real de la base.
    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>("postgres:17");
    }
}
