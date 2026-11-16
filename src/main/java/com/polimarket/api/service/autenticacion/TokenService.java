package com.polimarket.api.service.autenticacion;

import com.polimarket.api.entity.Usuario;
import java.time.Duration;

/**
 * Emision de access tokens JWT (RS256). La validacion de cada peticion la hace Spring Security
 * con el {@code JwtDecoder} configurado en la capa security.
 */
public interface TokenService {

    /** Emite un JWT con los claims iss, aud, sub, uid, roles, iat, exp y jti. */
    String emitirAccessToken(Usuario usuario);

    Duration vigenciaAccessToken();
}
