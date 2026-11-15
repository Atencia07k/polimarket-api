package com.polimarket.api.service.proveedores;

import com.polimarket.api.entity.Proveedor;
import java.util.List;

/**
 * Componente Proveedores: consulta de proveedores y actualizacion de su contacto.
 */
public interface ProveedorService {

    List<Proveedor> listarProveedores();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Proveedor obtenerProveedor(String nit);

    List<Proveedor> buscarProveedoresPorNombre(String nombre);

    /** Proveedores que suministran el producto (relacion producto_proveedor). */
    List<Proveedor> consultarProveedoresPorProducto(Integer idProducto);

    /**
     * @throws com.polimarket.api.exception.DatosInvalidosException si el contacto esta vacio
     */
    Proveedor actualizarContacto(String nit, String contacto);
}
