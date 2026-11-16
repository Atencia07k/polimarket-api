package com.polimarket.api.service.autenticacion;

/**
 * Par de tokens entregado al cliente tras un login o un refresh.
 *
 * @param accessToken            JWT firmado con RS256
 * @param expiraEnSegundos       vigencia del access token
 * @param refreshToken           valor opaco; la API solo guarda su hash
 * @param refreshExpiraEnSegundos vigencia del refresh token
 */
public record TokensEmitidos(String accessToken, long expiraEnSegundos,
                             String refreshToken, long refreshExpiraEnSegundos) {
}
