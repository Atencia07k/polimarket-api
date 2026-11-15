package com.polimarket.api.exception;

/**
 * Base de las excepciones de reglas de negocio. Es unchecked para que {@code @Transactional}
 * haga rollback automaticamente. La capa Controller decidira despues su traduccion a HTTP.
 */
public abstract class NegocioException extends RuntimeException {

    protected NegocioException(String mensaje) {
        super(mensaje);
    }
}
