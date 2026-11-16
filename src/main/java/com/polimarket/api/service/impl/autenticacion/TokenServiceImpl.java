package com.polimarket.api.service.impl.autenticacion;

import com.polimarket.api.entity.Usuario;
import com.polimarket.api.security.JwtProperties;
import com.polimarket.api.service.autenticacion.TokenService;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    static final String CLAIM_ROLES = "roles";
    static final String CLAIM_UID = "uid";

    private final JwtEncoder jwtEncoder;
    private final JwtProperties propiedades;

    @Override
    public String emitirAccessToken(Usuario usuario) {
        Instant ahora = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.issuer())
                .audience(List.of(propiedades.audience()))
                .subject(usuario.getUsername())
                .claim(CLAIM_UID, usuario.getId())
                .claim(CLAIM_ROLES, List.of(usuario.getRol().name()))
                .issuedAt(ahora)
                .notBefore(ahora)
                .expiresAt(ahora.plus(propiedades.accessTokenTtl()))
                .id(UUID.randomUUID().toString())
                .build();
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }

    @Override
    public Duration vigenciaAccessToken() {
        return propiedades.accessTokenTtl();
    }
}
