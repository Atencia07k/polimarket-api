package com.polimarket.api.service.rrhh;

import com.polimarket.api.entity.Vendedor;
import java.util.List;

/**
 * Componente Recursos Humanos: consulta de vendedores.
 */
public interface VendedorService {

    List<Vendedor> listarVendedores();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Vendedor obtenerVendedor(Integer idVendedor);

    List<Vendedor> buscarVendedoresPorNombre(String nombre);

    boolean existeVendedor(Integer idVendedor);
}
