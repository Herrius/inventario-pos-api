package com.enrique.inventario.common;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS para el frontend SPA (subdirectorio web/). Permite cualquier origen
 * por defecto — esto es razonable para un demo público que se llama desde
 * un dominio del frontend distinto al backend.
 *
 * Para producción con dominio fijo, setear APP_CORS_ALLOWED_ORIGINS con la
 * lista exacta (ej. "https://inventario.demo.com"). Default "*" no requiere
 * config en Railway/dev.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Si es "*" usamos allowedOriginPatterns (permite credentials con wildcard);
        // si es una lista, allowedOrigins exacto.
        if ("*".equals(allowedOrigins)) {
            cfg.setAllowedOriginPatterns(List.of("*"));
        } else {
            cfg.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        }
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        cfg.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
