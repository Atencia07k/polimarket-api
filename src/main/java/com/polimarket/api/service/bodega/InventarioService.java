package com.polimarket.api.service.bodega;

import com.polimarket.api.entity.CompraProveedor;
import com.polimarket.api.entity.Entrega;
import com.polimarket.api.entity.Inventario;
import com.polimarket.api.entity.MovimientoStock;
import java.util.List;

/**
 * Componente Bodega: existencias. Es el unico servicio que modifica el stock y crea movimientos,
 * siempre en la misma transaccion y con bloqueo pesimista sobre el registro de inventario.
 */
public interface InventarioService {

    /**
     * @throws com.polimarket.api.exception.RecursoNoEncontradoException si no hay inventario del producto en la bodega
     */
    int consultarStock(Integer idProducto, Integer idBodega);

    /** Suma de las existencias del producto en todas las bodegas. */
    int consultarStockTotal(Integer idProducto);

    List<Inventario> consultarInventarioPorProducto(Integer idProducto);

    List<Inventario> consultarInventarioPorBodega(Integer idBodega);

    /** Regla de {@link Inventario#requiereReposicion()}: stock &lt;= stockMinimo. */
    boolean requiereReposicion(Integer idProducto, Integer idBodega);

    List<Inventario> consultarInventariosQueRequierenReposicion();

    /**
     * Ingresa unidades de una compra recibida. El registro de inventario debe existir.
     *
     * @throws com.polimarket.api.exception.DatosInvalidosException si la cantidad no es positiva o falta la compra
     * @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe el producto, la bodega o su inventario
     */
    MovimientoStock registrarEntrada(Integer idProducto, Integer idBodega, int cantidad, CompraProveedor compra);

    /**
     * Retira unidades por una entrega confirmada.
     *
     * @throws com.polimarket.api.exception.DatosInvalidosException si la cantidad no es positiva o falta la entrega
     * @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe el producto, la bodega o su inventario
     * @throws com.polimarket.api.exception.InventarioInsuficienteException si el stock quedaria negativo
     */
    MovimientoStock registrarSalida(Integer idProducto, Integer idBodega, int cantidad, Entrega entrega);
}
