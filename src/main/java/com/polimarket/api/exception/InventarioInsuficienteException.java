package com.polimarket.api.exception;

/**
 * Las existencias no alcanzan para la cantidad solicitada.
 */
public class InventarioInsuficienteException extends NegocioException {

    public InventarioInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
