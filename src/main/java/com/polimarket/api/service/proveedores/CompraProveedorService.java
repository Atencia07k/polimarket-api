package com.polimarket.api.service.proveedores;

import com.polimarket.api.entity.CompraProveedor;
import java.util.List;

/**
 * Componente Proveedores: compras a proveedores.
 * <p>
 * Reglas aprobadas: la compra se registra en {@link #ESTADO_PENDIENTE} sin mover inventario;
 * al recibirla en una bodega se registran las entradas y pasa a {@link #ESTADO_RECIBIDA}.
 * Solo una compra PENDIENTE puede recibirse o cancelarse. Cada producto debe ser suministrado
 * por el proveedor de la compra y su precio es el costo informado al registrarla.
 */
public interface CompraProveedorService {

    String ESTADO_PENDIENTE = "PENDIENTE";
    String ESTADO_RECIBIDA = "RECIBIDA";

    /** Linea de la compra: producto, cantidad y precio de costo unitario. */
    record ItemCompra(Integer idProducto, int cantidad, double precio) {
    }

    /**
     * @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe el proveedor o algun producto
     * @throws com.polimarket.api.exception.DatosInvalidosException si los items son invalidos
     *         o algun producto no es suministrado por el proveedor
     */
    CompraProveedor registrarCompra(String nitProveedor, List<ItemCompra> items);

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    CompraProveedor consultarCompra(Integer idCompra);

    List<CompraProveedor> consultarComprasPorProveedor(String nitProveedor);

    List<CompraProveedor> consultarComprasPendientes();

    /** Total de la compra segun {@link CompraProveedor#calcularTotal()}. */
    double calcularTotal(Integer idCompra);

    /**
     * Registra en la bodega la entrada de todos los productos de la compra y la marca RECIBIDA.
     *
     * @throws com.polimarket.api.exception.OperacionNoPermitidaException si la compra no esta PENDIENTE
     */
    CompraProveedor recibirCompra(Integer idCompra, Integer idBodega);

    /** @throws com.polimarket.api.exception.OperacionNoPermitidaException si la compra no esta PENDIENTE */
    CompraProveedor cancelarCompra(Integer idCompra);
}
