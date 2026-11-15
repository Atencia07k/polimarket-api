package com.polimarket.api.exception;

/**
 * El vendedor no tiene una autorizacion vigente de Recursos Humanos.
 */
public class VendedorNoAutorizadoException extends NegocioException {

    public VendedorNoAutorizadoException(Integer idVendedor) {
        super("El vendedor " + idVendedor + " no tiene una autorizacion vigente de Recursos Humanos");
    }
}
