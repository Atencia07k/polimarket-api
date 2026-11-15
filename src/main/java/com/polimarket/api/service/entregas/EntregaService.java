package com.polimarket.api.service.entregas;

import com.polimarket.api.entity.Entrega;
import java.time.LocalDate;
import java.util.List;

/**
 * Componente Entregas.
 * <p>
 * Reglas aprobadas: {@link #registrarEntrega} crea la entrega en {@link #ESTADO_PENDIENTE} sin mover
 * inventario; {@link #confirmarEntrega} descuenta las existencias de la bodega indicada y la marca
 * {@link Entrega#ESTADO_ENTREGADA}. Solo una entrega PENDIENTE de una venta REGISTRADA puede
 * confirmarse, lo que impide descuentos duplicados.
 */
public interface EntregaService {

    String ESTADO_PENDIENTE = "PENDIENTE";

    List<Entrega> consultarPendientes();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    Entrega consultarEntrega(Integer idEntrega);

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si la venta no tiene entrega */
    Entrega consultarEntregaPorVenta(Integer idVenta);

    List<Entrega> consultarPorEstado(String estado);

    /** Rango inclusivo. */
    List<Entrega> consultarPorRangoFechas(LocalDate desde, LocalDate hasta);

    /**
     * @throws com.polimarket.api.exception.OperacionNoPermitidaException si la venta no esta REGISTRADA o ya tiene entrega
     */
    Entrega registrarEntrega(Integer idVenta, String direccion);

    /**
     * Registra la salida de todos los productos de la venta desde la bodega y marca la entrega ENTREGADA.
     *
     * @throws com.polimarket.api.exception.OperacionNoPermitidaException si la entrega no esta PENDIENTE
     *         o la venta no esta REGISTRADA
     * @throws com.polimarket.api.exception.InventarioInsuficienteException si la bodega no tiene existencias suficientes
     */
    Entrega confirmarEntrega(Integer idEntrega, Integer idBodega);
}
