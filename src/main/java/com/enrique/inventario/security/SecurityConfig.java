package com.enrique.inventario.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity  // habilita @PreAuthorize en los controllers/services
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationConverter jwtConverter) throws Exception {
        http
            // CORS habilitado: el bean CorsConfigurationSource de CorsConfig
            // se aplica automáticamente al filter chain.
            .cors(c -> {})
            // API stateless: el servidor no guarda sesión. El cliente se
            // identificará con un JWT en cada request (M1.3); no hay cookie de sesión.
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // CSRF protege flujos con sesión/cookie. Una API stateless con Bearer
            // token no lo necesita. (Si algún día usas cookies de sesión, NO lo apagues.)
            .csrf(csrf -> csrf.disable())
            // Spring Security 7: authorizeHttpRequests + lambda DSL.
            .authorizeHttpRequests(auth -> auth
                // Públicos: registro/login y las sondas de salud.
                .requestMatchers("/v1/auth/**").permitAll()
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                // El forward interno a /error (p. ej. en un 404) también pasa por
                // seguridad; permitirlo evita que un 404 legítimo termine como 403.
                .requestMatchers("/error").permitAll()
                // Swagger UI + OpenAPI spec (M1). Útiles para inspeccionar la API
                // sin login; no exponen datos.
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                // Inventario / POS NO es público: los productos son data interna del
                // negocio. Todo lo demás exige autenticación; ADMIN vs CAJERO se
                // resuelve por @PreAuthorize en los métodos.
                .anyRequest().authenticated()
            )
            // Es una API REST: sin login por formulario ni HTTP Basic.
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            // Habilita la validación de JWT (Bearer token). Esto además instala el
            // entry point que responde 401 a los no-autenticados (en vez del 403 por defecto).
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        // BCrypt: hash adaptativo con salt incorporado. El estándar para passwords.
        return new BCryptPasswordEncoder();
    }

    // Mapea el claim "role" del JWT a una authority ROLE_<role>, para que
    // @PreAuthorize("hasRole('ADMIN')") funcione. Por defecto el resource server
    // solo mapearía el claim "scope" a SCOPE_*.
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            String role = jwt.getClaimAsString("role");
            if (role == null) {
                return List.<GrantedAuthority>of();
            }
            return List.<GrantedAuthority>of(new SimpleGrantedAuthority("ROLE_" + role));
        });
        return converter;
    }
}
