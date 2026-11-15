package com.polimarket.api.exception;

/**
 * Los datos recibidos por el servicio no son validos.
 */
public class DatosInvalidosException extends NegocioException {

    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
