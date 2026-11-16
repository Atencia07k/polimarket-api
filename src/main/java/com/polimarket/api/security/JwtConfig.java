package com.polimarket.api.security;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.proc.SecurityContext;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.converter.RsaKeyConverters;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Llaves RSA y codificador/decodificador de JWT (RS256).
 * <p>
 * La llave privada solo firma tokens dentro de la API; la publica valida cada peticion. Si no se configuran
 * llaves y el perfil lo permite ({@code polimarket.jwt.ephemeral-keys-allowed=true}), se genera un par efimero.
 */
@Slf4j
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    RSAKey jwtRsaKey(JwtProperties propiedades, ResourceLoader resourceLoader) throws Exception {
        RSAPublicKey publica;
        RSAPrivateKey privada;
        if (propiedades.tieneLlavesConfiguradas()) {
            publica = leer(resourceLoader, propiedades.publicKeyLocation(), RsaKeyConverters.x509()::convert);
            privada = leer(resourceLoader, propiedades.privateKeyLocation(), RsaKeyConverters.pkcs8()::convert);
            if (!publica.getModulus().equals(privada.getModulus())) {
                throw new IllegalStateException("Las llaves JWT configuradas no forman un par RSA");
            }
            log.info("Llaves JWT RSA cargadas desde la configuracion");
        } else if (propiedades.ephemeralKeysAllowed()) {
            KeyPair par = generarParEfimero();
            publica = (RSAPublicKey) par.getPublic();
            privada = (RSAPrivateKey) par.getPrivate();
            log.warn("No se configuraron llaves JWT: se genero un par RSA efimero. "
                    + "Los tokens dejan de ser validos al reiniciar la API.");
        } else {
            throw new IllegalStateException("Faltan las llaves JWT: defina JWT_PRIVATE_KEY_LOCATION y "
                    + "JWT_PUBLIC_KEY_LOCATION (polimarket.jwt.private-key-location / public-key-location)");
        }
        if (publica.getModulus().bitLength() < 2048) {
            throw new IllegalStateException("La llave RSA para JWT debe tener al menos 2048 bits");
        }
        return new RSAKey.Builder(publica).privateKey(privada).keyIDFromThumbprint().build();
    }

    @Bean
    JwtEncoder jwtEncoder(RSAKey jwtRsaKey) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<SecurityContext>(new JWKSet(jwtRsaKey)));
    }

    @Bean
    JwtDecoder jwtDecoder(RSAKey jwtRsaKey, JwtProperties propiedades) throws Exception {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(jwtRsaKey.toRSAPublicKey())
                .signatureAlgorithm(SignatureAlgorithm.RS256)
                .build();
        decoder.setJwtValidator(validador(propiedades));
        return decoder;
    }

    static OAuth2TokenValidator<Jwt> validador(JwtProperties propiedades) {
        return new DelegatingOAuth2TokenValidator<>(List.of(
                new JwtTimestampValidator(propiedades.clockSkew()),
                new JwtIssuerValidator(propiedades.issuer()),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD,
                        aud -> aud != null && aud.contains(propiedades.audience())),
                new JwtClaimValidator<Object>(JwtClaimNames.EXP, exp -> exp != null),
                new JwtClaimValidator<String>(JwtClaimNames.SUB, sub -> sub != null && !sub.isBlank())));
    }

    private static <T> T leer(ResourceLoader resourceLoader, String ubicacion, LectorPem<T> lector)
            throws IOException {
        Resource recurso = ubicacion.startsWith("classpath:") || ubicacion.startsWith("file:")
                ? resourceLoader.getResource(ubicacion)
                : new FileSystemResource(ubicacion);
        if (!recurso.exists()) {
            throw new IllegalStateException("No existe el archivo de llave JWT: " + ubicacion);
        }
        try (InputStream entrada = recurso.getInputStream()) {
            return lector.leer(entrada);
        }
    }

    private static KeyPair generarParEfimero() throws NoSuchAlgorithmException {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        return generador.generateKeyPair();
    }

    @FunctionalInterface
    private interface LectorPem<T> {
        T leer(InputStream entrada);
    }
}
