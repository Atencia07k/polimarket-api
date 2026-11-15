package com.polimarket.api.exception;

/**
 * El registro solicitado no existe.
 */
public class RecursoNoEncontradoException extends NegocioException {

    public RecursoNoEncontradoException(String recurso, Object identificador) {
        super(recurso + " con identificador " + identificador + " no existe");
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
