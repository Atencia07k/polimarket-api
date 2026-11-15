package com.polimarket.api.exception;

/**
 * La operacion no esta permitida en el estado actual del registro.
 */
public class OperacionNoPermitidaException extends NegocioException {

    public OperacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
