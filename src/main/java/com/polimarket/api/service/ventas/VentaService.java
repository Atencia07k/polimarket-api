package com.polimarket.api.service.ventas;

import com.polimarket.api.entity.Cliente;
import com.polimarket.api.entity.Producto;
import com.polimarket.api.entity.Venta;
import java.util.List;

/**
 * Componente Ventas.
 * <p>
 * Reglas aprobadas: la venta se registra en {@link #ESTADO_REGISTRADA}; al registrarla solo se
 * verifica que la existencia total (todas las bodegas) alcance, sin descontar inventario: el
 * descuento ocurre al confirmar la entrega. El precio de cada detalle es el precio actual del producto.
 * Solo se cancela una venta REGISTRADA cuya entrega no este ENTREGADA.
 */
public interface VentaService {

    String ESTADO_REGISTRADA = "REGISTRADA";

    /** Linea de la venta: producto y cantidad solicitada. */
    record ItemVenta(Integer idProducto, int cantidad) {
    }

    List<Producto> listarProductos();

    List<Cliente> listarClientes();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Venta consultarVenta(Integer idVenta);

    List<Venta> consultarVentasPorVendedor(Integer idVendedor);

    List<Venta> consultarVentasPorCliente(Integer idCliente);

    /**
     * @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe el vendedor, el cliente o algun producto
     * @throws com.polimarket.api.exception.VendedorNoAutorizadoException si RRHH no autoriza al vendedor
     * @throws com.polimarket.api.exception.DatosInvalidosException si los items son invalidos
     * @throws com.polimarket.api.exception.InventarioInsuficienteException si la existencia total no alcanza
     */
    Venta registrarVenta(Integer idVendedor, Integer idCliente, List<ItemVenta> items);

    /** @throws com.polimarket.api.exception.OperacionNoPermitidaException si no cumple las condiciones de cancelacion */
    Venta cancelarVenta(Integer idVenta);
}
