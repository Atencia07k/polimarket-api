package com.polimarket.api.service.bodega;

import com.polimarket.api.entity.Producto;
import java.util.List;

/**
 * Componente Bodega: catalogo de productos. Unico punto de acceso a productos para los demas componentes.
 */
public interface ProductoService {

    List<Producto> listarProductos();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Producto obtenerProducto(Integer idProducto);

    List<Producto> buscarProductosPorNombre(String nombre);

    /**
     * @throws com.polimarket.api.exception.DatosInvalidosException si el precio es negativo o no numerico
     */
    Producto actualizarPrecio(Integer idProducto, double precio);

    /** Productos que suministra el proveedor (relacion producto_proveedor). */
    List<Producto> consultarProductosPorProveedor(String nitProveedor);

    boolean existeProducto(Integer idProducto);
}
