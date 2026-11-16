package com.polimarket.api.dto.autenticacion;

import com.polimarket.api.service.autenticacion.TokensEmitidos;

/**
 * Respuesta de login y refresh, con los nombres de campo habituales de OAuth 2.0 (RFC 6749).
 */
public record TokenResponse(String accessToken, String tokenType, long expiresIn,
                            String refreshToken, long refreshExpiresIn) {

    public static TokenResponse desde(TokensEmitidos tokens) {
        return new TokenResponse(tokens.accessToken(), "Bearer", tokens.expiraEnSegundos(),
                tokens.refreshToken(), tokens.refreshExpiraEnSegundos());
    }

    @Override
    public String toString() {
        return "TokenResponse[tokenType=Bearer, expiresIn=" + expiresIn + "]";
    }
}
