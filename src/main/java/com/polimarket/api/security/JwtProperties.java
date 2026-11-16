package com.polimarket.api.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion de los JWT emitidos por la API ({@code polimarket.jwt.*}).
 *
 * @param issuer                valor del claim {@code iss}
 * @param audience              valor exigido en el claim {@code aud}
 * @param accessTokenTtl        vigencia del access token
 * @param refreshTokenTtl       vigencia del refresh token
 * @param clockSkew             tolerancia de reloj al validar {@code exp}/{@code nbf}
 * @param privateKeyLocation    llave privada RSA en PEM (PKCS#8); ruta de archivo o recurso Spring
 * @param publicKeyLocation     llave publica RSA en PEM (X.509)
 * @param ephemeralKeysAllowed  si no hay llaves configuradas, genera un par efimero (solo dev/test)
 */
@ConfigurationProperties(prefix = "polimarket.jwt")
public record JwtProperties(
        String issuer,
        String audience,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        Duration clockSkew,
        String privateKeyLocation,
        String publicKeyLocation,
        boolean ephemeralKeysAllowed) {

    public JwtProperties {
        issuer = issuer == null || issuer.isBlank() ? "polimarket-api" : issuer;
        audience = audience == null || audience.isBlank() ? "polimarket-api" : audience;
        accessTokenTtl = accessTokenTtl == null ? Duration.ofMinutes(15) : accessTokenTtl;
        refreshTokenTtl = refreshTokenTtl == null ? Duration.ofDays(7) : refreshTokenTtl;
        clockSkew = clockSkew == null ? Duration.ofSeconds(30) : clockSkew;
    }

    boolean tieneLlavesConfiguradas() {
        return privateKeyLocation != null && !privateKeyLocation.isBlank()
                && publicKeyLocation != null && !publicKeyLocation.isBlank();
    }
}
