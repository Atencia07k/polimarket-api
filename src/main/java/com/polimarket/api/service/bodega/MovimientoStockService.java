package com.polimarket.api.service.bodega;

import com.polimarket.api.entity.MovimientoStock;
import java.time.LocalDate;
import java.util.List;

/**
 * Componente Bodega: consulta de movimientos de stock. Los movimientos solo se crean desde
 * {@link InventarioService} para que siempre acompanen la actualizacion de existencias.
 */
public interface MovimientoStockService {

    List<MovimientoStock> listarMovimientos();

    /** @throws com.polimarket.api.exception.RecursoNoEncontradoException si no existe */
    MovimientoStock obtenerMovimiento(Integer idMovimiento);

    List<MovimientoStock> consultarPorInventario(Integer idInventario);

    /** Movimientos del producto en todas las bodegas. */
    List<MovimientoStock> consultarPorProducto(Integer idProducto);

    /** Rango inclusivo. */
    List<MovimientoStock> consultarPorFechas(LocalDate desde, LocalDate hasta);

    /** @param tipo {@link MovimientoStock#TIPO_ENTRADA} o {@link MovimientoStock#TIPO_SALIDA} */
    List<MovimientoStock> consultarPorTipo(String tipo);

    List<MovimientoStock> consultarPorCompra(Integer idCompra);

    List<MovimientoStock> consultarPorEntrega(Integer idEntrega);
}
