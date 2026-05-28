package com.enrique.inventario.security;

import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/** Emite JWTs firmados (RS256) con el JwtEncoder configurado en JwtConfig. */
@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final long expirySeconds;

    // Recibe primitivos (subject, role), no la entidad User: el paquete security
    // no necesita conocer el modelo de dominio.
    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.jwt.expiry-seconds:3600}") long expirySeconds) {
        this.jwtEncoder = jwtEncoder;
        this.expirySeconds = expirySeconds;
    }

    public String issue(String subject, String role) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("inventario-pos-api")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(expirySeconds)) // token de vida corta
                .subject(subject)        // identifica al usuario (su email)
                .claim("role", role)     // lo usaremos para autorización en M1.4
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public long getExpirySeconds() {
        return expirySeconds;
    }
}
