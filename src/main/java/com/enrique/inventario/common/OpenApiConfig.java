package com.enrique.inventario.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata y security scheme global de la OpenAPI spec.
 *
 * - Declara JWT como esquema "bearerAuth" y lo aplica por defecto a todas las
 *   operaciones. Swagger UI muestra entonces el botón Authorize para pegar el
 *   token y los endpoints protegidos quedan ejecutables desde el navegador.
 * - Los endpoints públicos (/v1/auth/register, /v1/auth/login) no necesitan
 *   el header; los marcamos con @SecurityRequirements vacío en el controller
 *   si hace falta sobre-escribir el requerimiento global.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI inventarioOpenAPI() {
        final String bearer = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("Inventario / POS API")
                        .description("""
                                API REST de inventario + POS con stock event-sourced,
                                transacciones de venta atómicas y reportes con índices tuneados.

                                Roles: ADMIN (configuración + reportes), CAJERO (POS).
                                Auth: JWT Bearer en el header Authorization.
                                """)
                        .version("0.0.1"))
                .addSecurityItem(new SecurityRequirement().addList(bearer))
                .components(new Components()
                        .addSecuritySchemes(bearer, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
