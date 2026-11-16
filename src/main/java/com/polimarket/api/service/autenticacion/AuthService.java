package com.polimarket.api.service.autenticacion;

/**
 * Inicio de sesion, renovacion de tokens (refresh token rotativo) y cierre de sesion.
 */
public interface AuthService {

    /**
     * @throws com.polimarket.api.exception.AutenticacionFallidaException con mensaje generico si el usuario
     *         no existe, esta inactivo o la contrasena no coincide
     */
    TokensEmitidos iniciarSesion(String username, String password);

    /**
     * Rota el refresh token: el recibido queda revocado y se entrega un par nuevo de la misma familia.
     * Si el token ya habia sido usado (reutilizacion), se revoca toda la familia.
     *
     * @throws com.polimarket.api.exception.AutenticacionFallidaException si el token no es valido
     */
    TokensEmitidos refrescar(String refreshToken);

    /** Revoca la familia del refresh token. Es idempotente y no revela si el token existia. */
    void cerrarSesion(String refreshToken);
}
