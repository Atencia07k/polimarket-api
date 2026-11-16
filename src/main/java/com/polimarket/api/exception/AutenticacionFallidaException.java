package com.polimarket.api.exception;

/**
 * Credenciales o refresh token invalidos. El mensaje es siempre generico para no revelar
 * si un usuario existe ni por que se rechazo el token.
 */
public class AutenticacionFallidaException extends NegocioException {

    public AutenticacionFallidaException(String mensaje) {
        super(mensaje);
    }
}
